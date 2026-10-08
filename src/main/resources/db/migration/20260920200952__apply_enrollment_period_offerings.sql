CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE enrollment_periods ADD COLUMN scope_configured boolean NOT NULL DEFAULT false;
ALTER TABLE enrollment_periods ADD CONSTRAINT enrollment_periods_tenant_id_unique UNIQUE (institution_id, enrollment_period_id);

CREATE TABLE enrollment_period_offerings (
    offering_id uuid PRIMARY KEY,
    enrollment_period_id uuid NOT NULL REFERENCES enrollment_periods(enrollment_period_id),
    study_plan_id uuid NOT NULL REFERENCES study_plans(study_plan_id),
    CONSTRAINT enrollment_period_offerings_plan_unique UNIQUE(enrollment_period_id, study_plan_id),
    CONSTRAINT enrollment_period_offerings_id_plan_unique UNIQUE(offering_id, study_plan_id)
);

-- Dates and context are maintained by triggers, never accepted from clients.
-- A null academic level is an explicit reservation for unassigned spaces.
CREATE TABLE enrollment_period_offering_levels (
    offering_level_id uuid PRIMARY KEY,
    offering_id uuid NOT NULL,
    academic_level_id uuid,
    study_plan_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    enrollment_period_id uuid NOT NULL,
    availability tstzrange NOT NULL,
    reserves boolean NOT NULL,
    CONSTRAINT enrollment_period_level_offering_fk FOREIGN KEY(offering_id, study_plan_id)
        REFERENCES enrollment_period_offerings(offering_id, study_plan_id) ON DELETE CASCADE,
    CONSTRAINT enrollment_period_level_plan_fk FOREIGN KEY(study_plan_id, academic_level_id)
        REFERENCES academic_levels(study_plan_id, academic_level_id),
    CONSTRAINT enrollment_period_level_tenant_fk FOREIGN KEY(institution_id, enrollment_period_id)
        REFERENCES enrollment_periods(institution_id, enrollment_period_id),
    CONSTRAINT enrollment_period_level_unique UNIQUE NULLS NOT DISTINCT(offering_id, academic_level_id),
    CONSTRAINT enrollment_period_scope_overlap EXCLUDE USING gist (
        institution_id WITH =, academic_year_id WITH =, study_plan_id WITH =,
        (COALESCE(academic_level_id, '00000000-0000-0000-0000-000000000000'::uuid)) WITH =,
        availability WITH &&
    ) WHERE (reserves)
);

CREATE INDEX enrollment_period_levels_period_idx ON enrollment_period_offering_levels(enrollment_period_id);
CREATE INDEX enrollment_applications_period_idx ON enrollment_applications(enrollment_period_id);

CREATE FUNCTION validate_enrollment_period_offering() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE period enrollment_periods%ROWTYPE; plan study_plans%ROWTYPE;
BEGIN
    SELECT * INTO period FROM enrollment_periods WHERE enrollment_period_id = NEW.enrollment_period_id FOR UPDATE;
    SELECT * INTO plan FROM study_plans WHERE study_plan_id = NEW.study_plan_id;
    IF plan.institution_id IS DISTINCT FROM period.institution_id OR plan.status = 'DRAFT' OR plan.deleted_at IS NOT NULL THEN
        RAISE EXCEPTION 'Invalid period offering' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_period_scope_invalid';
    END IF;
    IF TG_OP = 'UPDATE' AND (OLD.enrollment_period_id <> NEW.enrollment_period_id OR OLD.study_plan_id <> NEW.study_plan_id) THEN
        RAISE EXCEPTION 'Offering context is immutable' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_period_scope_invalid';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER enrollment_period_offering_validate BEFORE INSERT OR UPDATE ON enrollment_period_offerings
    FOR EACH ROW EXECUTE FUNCTION validate_enrollment_period_offering();

CREATE FUNCTION derive_enrollment_period_level() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE period enrollment_periods%ROWTYPE; offering enrollment_period_offerings%ROWTYPE;
BEGIN
    IF TG_OP = 'UPDATE' AND (OLD.offering_id <> NEW.offering_id OR OLD.academic_level_id IS DISTINCT FROM NEW.academic_level_id) THEN
        RAISE EXCEPTION 'Scope context is immutable' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_period_scope_invalid';
    END IF;
    SELECT * INTO offering FROM enrollment_period_offerings WHERE offering_id = NEW.offering_id;
    SELECT * INTO period FROM enrollment_periods WHERE enrollment_period_id = offering.enrollment_period_id FOR UPDATE;
    NEW.study_plan_id := offering.study_plan_id;
    NEW.institution_id := period.institution_id;
    NEW.academic_year_id := period.academic_year_id;
    NEW.enrollment_period_id := period.enrollment_period_id;
    NEW.availability := tstzrange(period.start_date, period.end_date, '[]');
    NEW.reserves := period.deleted_at IS NULL AND period.status IN ('PLANNED', 'OPEN');
    RETURN NEW;
END $$;
CREATE TRIGGER enrollment_period_level_derive BEFORE INSERT OR UPDATE ON enrollment_period_offering_levels
    FOR EACH ROW EXECUTE FUNCTION derive_enrollment_period_level();

CREATE FUNCTION sync_enrollment_period_reservations() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE enrollment_period_offering_levels SET reserves = reserves WHERE enrollment_period_id = NEW.enrollment_period_id;
    RETURN NEW;
END $$;
CREATE TRIGGER enrollment_period_reservations_sync AFTER UPDATE OF start_date, end_date, status, deleted_at ON enrollment_periods
    FOR EACH ROW EXECUTE FUNCTION sync_enrollment_period_reservations();

CREATE FUNCTION prevent_enrollment_period_scope_shrink() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE period_id uuid; configured boolean;
BEGIN
    period_id := OLD.enrollment_period_id;
    SELECT scope_configured INTO configured FROM enrollment_periods WHERE enrollment_period_id = period_id FOR UPDATE;
    IF configured AND EXISTS (SELECT 1 FROM enrollment_applications WHERE enrollment_period_id = period_id) THEN
        RAISE EXCEPTION 'Scope cannot shrink after applications' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_period_scope_shrink';
    END IF;
    RETURN OLD;
END $$;
CREATE TRIGGER enrollment_period_offering_preserve BEFORE DELETE ON enrollment_period_offerings
    FOR EACH ROW EXECUTE FUNCTION prevent_enrollment_period_scope_shrink();
CREATE TRIGGER enrollment_period_level_preserve BEFORE DELETE ON enrollment_period_offering_levels
    FOR EACH ROW EXECUTE FUNCTION prevent_enrollment_period_scope_shrink();

DROP INDEX enrollment_apps_applicant_open_path_year_unique;
CREATE UNIQUE INDEX enrollment_apps_applicant_open_path_period_unique
    ON enrollment_applications(institution_id, applicant_person_id, training_path_id, enrollment_period_id)
    WHERE deleted_at IS NULL AND status = 'DRAFT';

CREATE FUNCTION preserve_enrollment_period_context() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.institution_id <> NEW.institution_id OR OLD.academic_year_id <> NEW.academic_year_id
        OR (OLD.scope_configured AND NOT NEW.scope_configured) THEN
        RAISE EXCEPTION 'Period context is immutable' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_period_scope_invalid';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER enrollment_period_context_preserve BEFORE UPDATE ON enrollment_periods
    FOR EACH ROW EXECUTE FUNCTION preserve_enrollment_period_context();

-- Deferred because Hibernate inserts the period before cascading its offerings.
CREATE FUNCTION require_configured_period_offerings() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE period_id uuid;
BEGIN
    IF TG_TABLE_NAME = 'enrollment_periods' THEN
        period_id := NEW.enrollment_period_id;
    ELSE
        period_id := OLD.enrollment_period_id;
    END IF;
    IF EXISTS (SELECT 1 FROM enrollment_periods WHERE enrollment_period_id = period_id AND scope_configured)
       AND (NOT EXISTS (SELECT 1 FROM enrollment_period_offering_levels WHERE enrollment_period_id = period_id)
         OR EXISTS (SELECT 1 FROM enrollment_period_offerings offering WHERE offering.enrollment_period_id = period_id
              AND NOT EXISTS (SELECT 1 FROM enrollment_period_offering_levels level WHERE level.offering_id = offering.offering_id))) THEN
        RAISE EXCEPTION 'Configured period requires an offering' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_period_scope_invalid';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER enrollment_period_scope_required AFTER INSERT OR UPDATE ON enrollment_periods
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION require_configured_period_offerings();
CREATE CONSTRAINT TRIGGER enrollment_period_scope_retained AFTER DELETE ON enrollment_period_offering_levels
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION require_configured_period_offerings();

CREATE FUNCTION validate_draft_course_period_scope() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE application enrollment_applications%ROWTYPE;
BEGIN
    SELECT * INTO application FROM enrollment_applications WHERE enrollment_application_id = NEW.enrollment_application_id;
    IF application.status = 'DRAFT' AND NOT EXISTS (
        SELECT 1 FROM courses course
        JOIN enrollment_period_offering_levels selection ON selection.enrollment_period_id = application.enrollment_period_id
        JOIN study_plan_spaces space ON space.study_plan_space_id = course.study_plan_space_id
        JOIN enrollment_periods period ON period.enrollment_period_id = selection.enrollment_period_id
        WHERE course.course_id = NEW.course_id AND course.institution_id = application.institution_id
          AND course.academic_year_id = application.academic_year_id
          AND selection.study_plan_id = space.study_plan_id
          AND selection.academic_level_id IS NOT DISTINCT FROM space.academic_level_id
          AND period.scope_configured
    ) THEN
        RAISE EXCEPTION 'Course is outside the period scope' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_course_period_scope_check';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER enrollment_draft_course_period_scope BEFORE INSERT OR UPDATE OF course_id, enrollment_application_id
    ON enrollment_application_courses FOR EACH ROW EXECUTE FUNCTION validate_draft_course_period_scope();

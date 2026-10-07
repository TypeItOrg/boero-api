-- Historical applications retain their original context. New drafts are scoped by path.
ALTER TABLE enrollment_applications ALTER COLUMN enrollment_period_id DROP NOT NULL;
ALTER TABLE enrollment_applications ALTER COLUMN academic_year_id DROP NOT NULL;

ALTER TABLE enrollment_application_courses ADD COLUMN enrollment_period_id uuid;
UPDATE enrollment_application_courses selection SET enrollment_period_id = application.enrollment_period_id
FROM enrollment_applications application WHERE application.enrollment_application_id = selection.enrollment_application_id;
ALTER TABLE enrollment_application_courses ALTER COLUMN enrollment_period_id SET NOT NULL;
ALTER TABLE enrollment_application_courses ADD CONSTRAINT enrollment_application_course_period_tenant_fk
    FOREIGN KEY (institution_id, enrollment_period_id) REFERENCES enrollment_periods(institution_id, enrollment_period_id);
CREATE INDEX enrollment_application_courses_period_idx ON enrollment_application_courses(enrollment_period_id);

-- Keep a permanent record of periods used, including selections later removed from drafts.
CREATE TABLE enrollment_application_periods (
    enrollment_application_id uuid NOT NULL REFERENCES enrollment_applications(enrollment_application_id),
    enrollment_period_id uuid NOT NULL REFERENCES enrollment_periods(enrollment_period_id),
    PRIMARY KEY(enrollment_application_id, enrollment_period_id)
);
CREATE INDEX enrollment_application_periods_period_idx ON enrollment_application_periods(enrollment_period_id);
INSERT INTO enrollment_application_periods SELECT enrollment_application_id, enrollment_period_id
FROM enrollment_applications WHERE enrollment_period_id IS NOT NULL;

CREATE UNIQUE INDEX enrollment_apps_applicant_open_path_unique
    ON enrollment_applications(institution_id, applicant_person_id, training_path_id)
    WHERE deleted_at IS NULL AND status = 'DRAFT' AND enrollment_period_id IS NULL;

CREATE OR REPLACE FUNCTION validate_course_selection_integrity() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent enrollment_applications%ROWTYPE; selected courses%ROWTYPE;
BEGIN
    PERFORM 1 FROM institutions WHERE institution_id = NEW.institution_id FOR UPDATE;
    SELECT * INTO parent FROM enrollment_applications WHERE enrollment_application_id = NEW.enrollment_application_id;
    SELECT * INTO selected FROM courses WHERE course_id = NEW.course_id;
    IF parent.institution_id IS DISTINCT FROM NEW.institution_id
       OR selected.institution_id IS DISTINCT FROM NEW.institution_id
       OR (parent.academic_year_id IS NOT NULL AND selected.academic_year_id IS DISTINCT FROM parent.academic_year_id)
       OR selected.training_path_id IS DISTINCT FROM parent.training_path_id THEN
        RAISE EXCEPTION 'Invalid course context' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_application_courses_context_check';
    END IF;
    IF NEW.status IN ('PENDING', 'WAITLISTED') AND parent.status NOT IN ('CANCELLED', 'REJECTED')
       AND EXISTS (SELECT 1 FROM enrollment_application_courses other
           JOIN enrollment_applications app ON app.enrollment_application_id = other.enrollment_application_id
           WHERE other.institution_id = NEW.institution_id AND other.course_id = NEW.course_id
             AND other.enrollment_application_course_id <> NEW.enrollment_application_course_id
             AND app.applicant_person_id = parent.applicant_person_id AND app.deleted_at IS NULL
             AND app.status NOT IN ('CANCELLED', 'REJECTED') AND other.status IN ('PENDING', 'WAITLISTED')) THEN
        RAISE EXCEPTION 'Course already requested' USING ERRCODE = '23505', CONSTRAINT = 'enrollment_application_courses_active_applicant_course_unique';
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION validate_draft_course_period_scope() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE application enrollment_applications%ROWTYPE;
BEGIN
    SELECT * INTO application FROM enrollment_applications WHERE enrollment_application_id = NEW.enrollment_application_id;
    IF TG_OP = 'UPDATE' AND OLD.enrollment_period_id IS DISTINCT FROM NEW.enrollment_period_id THEN
        RAISE EXCEPTION 'The assigned period is immutable' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_course_period_scope_check';
    END IF;
    IF application.status = 'DRAFT' AND NOT EXISTS (
        SELECT 1 FROM courses course
        JOIN study_plan_spaces space ON space.study_plan_space_id = course.study_plan_space_id
        JOIN enrollment_period_offering_levels selection ON selection.enrollment_period_id = NEW.enrollment_period_id
        JOIN enrollment_periods period ON period.enrollment_period_id = selection.enrollment_period_id
        WHERE course.course_id = NEW.course_id AND course.institution_id = application.institution_id
          AND course.academic_year_id = period.academic_year_id
          AND (application.enrollment_period_id IS NULL OR application.enrollment_period_id = period.enrollment_period_id)
          AND selection.study_plan_id = space.study_plan_id
          AND selection.academic_level_id IS NOT DISTINCT FROM space.academic_level_id
          AND period.scope_configured AND period.deleted_at IS NULL AND period.status = 'OPEN'
          AND period.start_date <= CURRENT_TIMESTAMP AND period.end_date >= CURRENT_TIMESTAMP
    ) THEN
        RAISE EXCEPTION 'Course is outside its open period scope' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_course_period_scope_check';
    END IF;
    INSERT INTO enrollment_application_periods VALUES (NEW.enrollment_application_id, NEW.enrollment_period_id) ON CONFLICT DO NOTHING;
    RETURN NEW;
END $$;
DROP TRIGGER enrollment_draft_course_period_scope ON enrollment_application_courses;
CREATE TRIGGER enrollment_draft_course_period_scope BEFORE INSERT OR UPDATE OF course_id, enrollment_application_id, enrollment_period_id
    ON enrollment_application_courses FOR EACH ROW EXECUTE FUNCTION validate_draft_course_period_scope();

CREATE OR REPLACE FUNCTION prevent_enrollment_period_scope_shrink() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE period_id uuid; configured boolean;
BEGIN
    period_id := OLD.enrollment_period_id;
    SELECT scope_configured INTO configured FROM enrollment_periods WHERE enrollment_period_id = period_id FOR UPDATE;
    IF configured AND EXISTS (SELECT 1 FROM enrollment_application_periods WHERE enrollment_period_id = period_id) THEN
        RAISE EXCEPTION 'Scope cannot shrink after applications' USING ERRCODE = '23514', CONSTRAINT = 'enrollment_period_scope_shrink';
    END IF;
    RETURN OLD;
END $$;

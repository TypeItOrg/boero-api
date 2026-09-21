-- Only currently submitted, non-deleted applications prevent reducing a period.
-- Historical period links alone are not evidence that a submitted request uses it.
CREATE OR REPLACE FUNCTION prevent_enrollment_period_scope_shrink() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE period_id uuid; configured boolean;
BEGIN
    period_id := OLD.enrollment_period_id;
    SELECT scope_configured INTO configured FROM enrollment_periods WHERE enrollment_period_id = period_id FOR UPDATE;
    IF configured AND EXISTS (
        SELECT 1 FROM enrollment_applications application
        WHERE application.status = 'SUBMITTED' AND application.deleted_at IS NULL
          AND (application.enrollment_period_id = period_id OR EXISTS (
              SELECT 1 FROM enrollment_application_courses selection
              WHERE selection.enrollment_application_id = application.enrollment_application_id
                AND selection.enrollment_period_id = period_id))
    ) THEN
        RAISE EXCEPTION 'Scope cannot shrink with submitted applications'
            USING ERRCODE = '23514', CONSTRAINT = 'enrollment_period_scope_shrink';
    END IF;
    RETURN OLD;
END $$;

-- This also runs for levels cascaded by removal of an entire plan offering.
-- Match the assigned period and the exact curriculum placement, including unassigned spaces.
CREATE FUNCTION clear_draft_courses_after_period_scope_reduction() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    WITH removed AS (
        DELETE FROM enrollment_application_courses selection
        USING enrollment_applications application, courses course, study_plan_spaces space
        WHERE application.enrollment_application_id = selection.enrollment_application_id
          AND application.status = 'DRAFT' AND application.deleted_at IS NULL
          AND selection.enrollment_period_id = OLD.enrollment_period_id
          AND course.course_id = selection.course_id
          AND space.study_plan_space_id = course.study_plan_space_id
          AND space.study_plan_id = OLD.study_plan_id
          AND space.academic_level_id IS NOT DISTINCT FROM OLD.academic_level_id
        RETURNING selection.enrollment_application_id
    )
    UPDATE enrollment_applications SET updated_at = CURRENT_TIMESTAMP
    WHERE enrollment_application_id IN (SELECT enrollment_application_id FROM removed);
    RETURN NULL;
END $$;
CREATE TRIGGER enrollment_period_scope_clear_drafts
    AFTER DELETE ON enrollment_period_offering_levels
    FOR EACH ROW EXECUTE FUNCTION clear_draft_courses_after_period_scope_reduction();

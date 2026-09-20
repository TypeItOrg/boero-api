-- Deleted courses remain historical records and do not freeze instrumental status.
-- Format changes still affect historical class schedules and retain their existing guard.
CREATE OR REPLACE FUNCTION protect_instantiated_academic_space() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.instrumental IS DISTINCT FROM OLD.instrumental
        AND EXISTS (SELECT 1 FROM courses
            WHERE institution_id = NEW.institution_id
              AND academic_space_id = NEW.academic_space_id AND deleted_at IS NULL))
       OR (NEW.format IS DISTINCT FROM OLD.format
        AND EXISTS (SELECT 1 FROM courses
            WHERE institution_id = NEW.institution_id
              AND academic_space_id = NEW.academic_space_id)) THEN
        RAISE EXCEPTION 'Academic space already instantiated'
            USING ERRCODE = '23514', CONSTRAINT = 'academic_spaces_instantiated_shape_check';
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION validate_course_instrument_integrity() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE placement study_plan_spaces%ROWTYPE; space academic_spaces%ROWTYPE; path uuid;
BEGIN
    SELECT * INTO placement FROM study_plan_spaces WHERE study_plan_space_id = NEW.study_plan_space_id;
    SELECT * INTO space FROM academic_spaces WHERE academic_space_id = placement.academic_space_id FOR SHARE;
    SELECT training_path_id INTO path FROM study_plans WHERE study_plan_id = placement.study_plan_id;
    IF placement.institution_id IS DISTINCT FROM NEW.institution_id
       OR placement.academic_space_id IS DISTINCT FROM NEW.academic_space_id
       OR placement.academic_level_id IS DISTINCT FROM NEW.academic_level_id
       OR path IS DISTINCT FROM NEW.training_path_id
       OR (NEW.deleted_at IS NULL AND space.instrumental IS DISTINCT FROM (NEW.instrument_id IS NOT NULL)) THEN
        RAISE EXCEPTION 'Invalid curriculum or instrument'
            USING ERRCODE = '23514', CONSTRAINT = 'courses_curriculum_instrument_check';
    END IF;
    RETURN NEW;
END $$;

-- Restoring a course must revalidate its instrument against the current space.
DROP TRIGGER courses_curriculum_integrity ON courses;
CREATE TRIGGER courses_curriculum_integrity
    BEFORE INSERT OR UPDATE OF study_plan_space_id, academic_space_id, academic_level_id,
        training_path_id, instrument_id, deleted_at
    ON courses FOR EACH ROW EXECUTE FUNCTION validate_course_instrument_integrity();

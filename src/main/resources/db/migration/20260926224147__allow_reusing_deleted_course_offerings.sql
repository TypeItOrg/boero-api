-- Keep historical courses without reserving the natural key of an active offering.
-- Restoring a deleted course still conflicts if its offering has been recreated.
DROP INDEX courses_curriculum_level_unique;
DROP INDEX courses_curriculum_without_level_unique;
DROP INDEX courses_instrumental_curriculum_level_unique;
DROP INDEX courses_instrumental_curriculum_without_level_unique;

CREATE UNIQUE INDEX courses_curriculum_level_unique
    ON courses (institution_id, training_path_id, academic_space_id, academic_level_id, academic_year_id)
    WHERE deleted_at IS NULL
      AND instrument_id IS NULL
      AND academic_level_id IS NOT NULL;

CREATE UNIQUE INDEX courses_curriculum_without_level_unique
    ON courses (institution_id, training_path_id, academic_space_id, academic_year_id)
    WHERE deleted_at IS NULL
      AND instrument_id IS NULL
      AND academic_level_id IS NULL;

CREATE UNIQUE INDEX courses_instrumental_curriculum_level_unique
    ON courses (institution_id, training_path_id, academic_space_id, academic_level_id, academic_year_id, instrument_id)
    WHERE deleted_at IS NULL
      AND instrument_id IS NOT NULL
      AND academic_level_id IS NOT NULL;

CREATE UNIQUE INDEX courses_instrumental_curriculum_without_level_unique
    ON courses (institution_id, training_path_id, academic_space_id, academic_year_id, instrument_id)
    WHERE deleted_at IS NULL
      AND instrument_id IS NOT NULL
      AND academic_level_id IS NULL;


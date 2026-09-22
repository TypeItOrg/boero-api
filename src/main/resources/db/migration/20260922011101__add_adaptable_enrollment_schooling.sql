ALTER TABLE applicant_education_backgrounds
  ADD COLUMN currently_studying BOOLEAN,
  ADD COLUMN education_level VARCHAR(30),
  ADD COLUMN level_completed BOOLEAN,
  ALTER COLUMN secondary_completed DROP NOT NULL,
  ALTER COLUMN secondary_completed DROP DEFAULT;

ALTER TABLE applicant_education_backgrounds
  ADD CONSTRAINT applicant_education_background_education_level_check
  CHECK (
    education_level IS NULL
    OR education_level IN (
      'NO_SCHOOLING',
      'INITIAL',
      'PRIMARY',
      'SECONDARY',
      'NON_UNIVERSITY_HIGHER',
      'UNIVERSITY'
    )
  );

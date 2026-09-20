-- Blocking now applies per course instead of per training path: several SUBMITTED
-- applications may coexist for the same person, path and year. Only a single open
-- DRAFT per context is kept so applicants resume rather than duplicate drafts.
DROP INDEX IF EXISTS enrollment_apps_applicant_open_path_year_unique;

CREATE UNIQUE INDEX enrollment_apps_applicant_open_path_year_unique
    ON enrollment_applications (institution_id, applicant_person_id, training_path_id, academic_year_id)
    WHERE deleted_at IS NULL
      AND status = 'DRAFT';

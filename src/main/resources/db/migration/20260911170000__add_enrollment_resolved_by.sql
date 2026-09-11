ALTER TABLE enrollment_applications ADD COLUMN IF NOT EXISTS resolved_by_person_id uuid;
ALTER TABLE enrollment_applications DROP CONSTRAINT IF EXISTS enrollment_applications_resolved_by_fk;
ALTER TABLE enrollment_applications ADD CONSTRAINT enrollment_applications_resolved_by_fk
    FOREIGN KEY (resolved_by_person_id) REFERENCES people (person_id);

CREATE SEQUENCE IF NOT EXISTS student_file_number_seq;

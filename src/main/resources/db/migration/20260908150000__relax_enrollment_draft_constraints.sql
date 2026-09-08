-- Un DRAFT es parcial por definición: la obligatoriedad de estos campos ya la
-- valida EnrollmentApplicationService.submitApplication al momento de enviar.

ALTER TABLE applicant_responsibles
    ALTER COLUMN full_name DROP NOT NULL,
    ALTER COLUMN document_number DROP NOT NULL,
    ALTER COLUMN phone_number DROP NOT NULL;

ALTER TABLE applicant_preferences
    ALTER COLUMN preferred_shift DROP NOT NULL;

CREATE INDEX IF NOT EXISTS enrollment_att_application_idx
    ON enrollment_attachments (enrollment_application_id);

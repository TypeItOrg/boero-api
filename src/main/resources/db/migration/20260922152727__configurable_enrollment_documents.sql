CREATE TABLE training_path_document_requirements (
 id uuid PRIMARY KEY, training_path_id uuid NOT NULL REFERENCES training_paths(training_path_id),
 name varchar(150) NOT NULL CHECK (length(trim(name)) > 0), instructions varchar(1000) NOT NULL,
 level varchar(40) NOT NULL CHECK (level IN ('AT_SUBMISSION','BEFORE_CONFIRMATION','OPTIONAL')),
 allowed_formats jsonb NOT NULL CHECK (jsonb_typeof(allowed_formats)='array' AND jsonb_array_length(allowed_formats)>0 AND allowed_formats <@ '["application/pdf","image/jpeg","image/png"]'::jsonb),
 display_order integer NOT NULL CHECK (display_order>=0), active boolean NOT NULL
);
CREATE INDEX training_path_document_requirements_path_idx ON training_path_document_requirements(training_path_id,display_order,id);
CREATE TABLE enrollment_document_requirements (
 id uuid PRIMARY KEY, application_id uuid NOT NULL REFERENCES enrollment_applications(enrollment_application_id),
 source_requirement_id uuid NOT NULL, name varchar(150) NOT NULL, instructions varchar(1000) NOT NULL,
 level varchar(40) NOT NULL CHECK (level IN ('AT_SUBMISSION','BEFORE_CONFIRMATION','OPTIONAL')),
 allowed_formats jsonb NOT NULL CHECK (jsonb_typeof(allowed_formats)='array' AND jsonb_array_length(allowed_formats)>0 AND allowed_formats <@ '["application/pdf","image/jpeg","image/png"]'::jsonb),
 display_order integer NOT NULL CHECK (display_order>=0), UNIQUE(id,application_id), UNIQUE(application_id,source_requirement_id)
);
-- This schema requires a clean attachment dataset; no legacy document backfill or data deletion.
DROP INDEX enrollment_attachments_active_type_unique;
ALTER TABLE enrollment_attachments DROP COLUMN attachment_type;
ALTER TABLE enrollment_attachments
 ADD COLUMN requirement_id uuid NOT NULL,
 ADD COLUMN version_status varchar(20) NOT NULL DEFAULT 'CURRENT' CHECK (version_status IN ('CURRENT','SUPERSEDED','WITHDRAWN')),
 ADD COLUMN review_status varchar(20) NOT NULL DEFAULT 'PENDING_REVIEW' CHECK (review_status IN ('PENDING_REVIEW','OBSERVED','ACCEPTED')),
 ADD COLUMN uploaded_by uuid,
 ADD COLUMN uploader_type varchar(20) NOT NULL,
 ADD COLUMN reviewed_by uuid,
 ADD COLUMN reviewer_type varchar(20),
 ADD COLUMN reviewed_at timestamptz,
 ADD COLUMN observation varchar(2000),
 ADD CONSTRAINT enrollment_attachment_requirement_fk FOREIGN KEY(requirement_id,enrollment_application_id) REFERENCES enrollment_document_requirements(id,application_id),
 ADD CONSTRAINT enrollment_attachment_review_check CHECK (
   (review_status='PENDING_REVIEW' AND reviewed_at IS NULL AND reviewer_type IS NULL)
   OR (review_status IN ('ACCEPTED','OBSERVED') AND reviewed_at IS NOT NULL AND reviewer_type IS NOT NULL)),
 ADD CONSTRAINT enrollment_attachment_observation_check CHECK (review_status<>'OBSERVED' OR length(trim(observation))>0 AND observation IS NOT NULL);
CREATE UNIQUE INDEX enrollment_attachments_current_requirement_unique ON enrollment_attachments(requirement_id) WHERE version_status='CURRENT' AND deleted_at IS NULL;
CREATE INDEX enrollment_attachment_history_idx ON enrollment_attachments(enrollment_application_id,requirement_id,created_at DESC);
ALTER TABLE enrollment_applications DROP CONSTRAINT enrollment_applications_status_check;
ALTER TABLE enrollment_applications ADD CONSTRAINT enrollment_applications_status_check CHECK (status IN ('DRAFT','SUBMITTED','PROVISIONALLY_APPROVED','APPROVED','REJECTED','CANCELLED'));
ALTER TABLE enrollment_document_audit DROP CONSTRAINT enrollment_document_audit_action_check;
ALTER TABLE enrollment_document_audit ADD CONSTRAINT enrollment_document_audit_action_check CHECK (action IN ('METADATA_READ','CONTENT_ACCESS','UPLOAD','REPLACE','DELETE','PHYSICAL_DELETE','REVIEW','PROVISIONAL_APPROVAL','FINAL_APPROVAL'));
CREATE TABLE enrollment_admission_history (
 id uuid PRIMARY KEY, application_id uuid NOT NULL REFERENCES enrollment_applications(enrollment_application_id),
 status varchar(40) NOT NULL, occurred_at timestamptz NOT NULL, actor_id uuid, account_type varchar(20) NOT NULL
);
CREATE INDEX enrollment_admission_history_application_idx ON enrollment_admission_history(application_id,occurred_at,id);

CREATE OR REPLACE FUNCTION validate_course_enrollment_parent() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.source = 'APPLICATION' AND NOT EXISTS (
        SELECT 1 FROM enrollment_application_courses child
        JOIN enrollment_applications parent USING (enrollment_application_id)
        JOIN students student ON student.person_id = parent.applicant_person_id AND student.institution_id = parent.institution_id
        WHERE child.enrollment_application_course_id = NEW.enrollment_application_course_id
          AND child.enrollment_application_id = NEW.enrollment_application_id AND child.course_id = NEW.course_id
          AND parent.status IN ('APPROVED','PROVISIONALLY_APPROVED') AND student.student_id = NEW.student_id) THEN
        RAISE EXCEPTION 'Invalid enrollment origin' USING ERRCODE = '23514', CONSTRAINT = 'course_enrollments_origin_check';
    END IF;
    RETURN NEW;
END $$;

CREATE FUNCTION validate_enrollment_document_gate() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.status NOT IN ('SUBMITTED','PROVISIONALLY_APPROVED','APPROVED') OR (TG_OP='UPDATE' AND OLD.status=NEW.status) THEN RETURN NEW; END IF;
 IF EXISTS (
  SELECT 1 FROM enrollment_document_requirements requirement
  WHERE requirement.application_id=NEW.enrollment_application_id
   AND (requirement.level='AT_SUBMISSION' OR NEW.status='APPROVED' AND requirement.level='BEFORE_CONFIRMATION')
   AND NOT EXISTS (SELECT 1 FROM enrollment_attachments attachment
    WHERE attachment.requirement_id=requirement.id AND attachment.version_status='CURRENT' AND attachment.deleted_at IS NULL
      AND (NEW.status='SUBMITTED' OR attachment.review_status='ACCEPTED'))
 ) THEN RAISE EXCEPTION 'Required documentation incomplete' USING ERRCODE='23514',CONSTRAINT='enrollment_document_gate_check'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER enrollment_document_gate BEFORE INSERT OR UPDATE OF status ON enrollment_applications FOR EACH ROW EXECUTE FUNCTION validate_enrollment_document_gate();

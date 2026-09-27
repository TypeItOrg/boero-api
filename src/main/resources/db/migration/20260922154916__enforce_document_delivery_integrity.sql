DROP TRIGGER enrollment_application_academic_requirements ON enrollment_applications;
DROP TRIGGER enrollment_document_gate ON enrollment_applications;
ALTER TABLE enrollment_applications ALTER COLUMN status TYPE varchar(40);
CREATE TRIGGER enrollment_application_academic_requirements BEFORE INSERT OR UPDATE OF status
 ON enrollment_applications FOR EACH ROW EXECUTE FUNCTION validate_application_academic_requirements();
CREATE TRIGGER enrollment_document_gate BEFORE INSERT OR UPDATE OF status
 ON enrollment_applications FOR EACH ROW EXECUTE FUNCTION validate_enrollment_document_gate();

CREATE FUNCTION protect_enrollment_document_delivery() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_status varchar(40);
BEGIN
 SELECT status INTO parent_status FROM enrollment_applications
 WHERE enrollment_application_id=NEW.enrollment_application_id FOR UPDATE;
 IF parent_status NOT IN ('DRAFT','SUBMITTED','PROVISIONALLY_APPROVED') THEN
  RAISE EXCEPTION 'Document delivery is locked' USING ERRCODE='23514',CONSTRAINT='enrollment_attachment_immutable_check';
 END IF;
 IF TG_OP='UPDATE' AND (
   OLD.version_status <> 'CURRENT' OR OLD.review_status='ACCEPTED'
   OR OLD.requirement_id IS DISTINCT FROM NEW.requirement_id
   OR OLD.enrollment_application_id IS DISTINCT FROM NEW.enrollment_application_id
   OR OLD.storage_path IS DISTINCT FROM NEW.storage_path
   OR OLD.original_file_name IS DISTINCT FROM NEW.original_file_name
   OR OLD.content_type IS DISTINCT FROM NEW.content_type
   OR OLD.file_size IS DISTINCT FROM NEW.file_size
   OR OLD.uploaded_by IS DISTINCT FROM NEW.uploaded_by
   OR OLD.uploader_type IS DISTINCT FROM NEW.uploader_type
   OR OLD.deleted_at IS DISTINCT FROM NEW.deleted_at
 ) THEN
  RAISE EXCEPTION 'Document delivery is immutable' USING ERRCODE='23514',CONSTRAINT='enrollment_attachment_immutable_check';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER enrollment_attachment_immutable BEFORE INSERT OR UPDATE ON enrollment_attachments
 FOR EACH ROW EXECUTE FUNCTION protect_enrollment_document_delivery();

CREATE FUNCTION check_enrollment_document_delivery_gate() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent enrollment_applications%ROWTYPE;
BEGIN
 SELECT * INTO parent FROM enrollment_applications WHERE enrollment_application_id=NEW.enrollment_application_id;
 IF parent.status IN ('SUBMITTED','PROVISIONALLY_APPROVED','APPROVED') AND EXISTS (
  SELECT 1 FROM enrollment_document_requirements requirement
  WHERE requirement.application_id=parent.enrollment_application_id
   AND (requirement.level='AT_SUBMISSION' OR parent.status='APPROVED' AND requirement.level='BEFORE_CONFIRMATION')
   AND NOT EXISTS (SELECT 1 FROM enrollment_attachments attachment
    WHERE attachment.requirement_id=requirement.id AND attachment.version_status='CURRENT' AND attachment.deleted_at IS NULL
      AND (parent.status='SUBMITTED' OR attachment.review_status='ACCEPTED'))
 ) THEN
  RAISE EXCEPTION 'Required documentation incomplete' USING ERRCODE='23514',CONSTRAINT='enrollment_document_gate_check';
 END IF;
 RETURN NULL;
END $$;
-- Evaluate after the replacement inserts the new delivery in the same transaction.
CREATE CONSTRAINT TRIGGER enrollment_attachment_parent_gate AFTER INSERT OR UPDATE ON enrollment_attachments
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_enrollment_document_delivery_gate();

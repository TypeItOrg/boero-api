CREATE TABLE enrollment_document_requests (
 id uuid PRIMARY KEY, application_id uuid NOT NULL, institution_id uuid NOT NULL,
 reason varchar(2000) NOT NULL CHECK(length(trim(reason))>0),created_at timestamptz NOT NULL,
 actor_id uuid NOT NULL,account_type varchar(20) NOT NULL CHECK(account_type IN ('INSTITUTION','PLATFORM')),actor_name varchar(300) NOT NULL,
 UNIQUE(id,application_id,institution_id),
 FOREIGN KEY(institution_id,application_id) REFERENCES enrollment_applications(institution_id,enrollment_application_id)
);
CREATE INDEX enrollment_document_requests_application_idx ON enrollment_document_requests(application_id,created_at,id);
ALTER TABLE enrollment_document_requirements ALTER COLUMN source_requirement_id DROP NOT NULL,
 ADD COLUMN origin varchar(20) NOT NULL DEFAULT 'ORIGINAL',ADD COLUMN request_id uuid,
 ADD CONSTRAINT enrollment_requirement_origin_check CHECK(
 (origin='ORIGINAL' AND source_requirement_id IS NOT NULL AND request_id IS NULL)
 OR (origin='ADDITIONAL' AND source_requirement_id IS NULL AND request_id IS NOT NULL)),
 ADD CONSTRAINT enrollment_requirement_request_tenant_fk FOREIGN KEY(request_id,application_id,institution_id) REFERENCES enrollment_document_requests(id,application_id,institution_id);
CREATE FUNCTION protect_enrollment_document_request() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_status varchar(40);
BEGIN
 IF TG_OP <> 'INSERT' THEN RAISE EXCEPTION 'Document request is immutable' USING ERRCODE='23514',CONSTRAINT='enrollment_document_request_immutable_check'; END IF;
 SELECT status INTO parent_status FROM enrollment_applications WHERE enrollment_application_id=NEW.application_id FOR UPDATE;
 IF parent_status NOT IN ('SUBMITTED','PROVISIONALLY_APPROVED') THEN RAISE EXCEPTION 'Document request is closed' USING ERRCODE='23514',CONSTRAINT='enrollment_document_request_closed_check'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER enrollment_document_request_immutable BEFORE INSERT OR UPDATE OR DELETE ON enrollment_document_requests FOR EACH ROW EXECUTE FUNCTION protect_enrollment_document_request();
CREATE OR REPLACE FUNCTION protect_enrollment_requirement_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_status varchar(40);
BEGIN
 SELECT status INTO parent_status FROM enrollment_applications WHERE enrollment_application_id=NEW.application_id FOR UPDATE;
 IF TG_OP='INSERT' AND NEW.origin='ADDITIONAL' THEN
  IF parent_status NOT IN ('SUBMITTED','PROVISIONALLY_APPROVED') THEN RAISE EXCEPTION 'Document request is closed' USING ERRCODE='23514',CONSTRAINT='enrollment_document_request_closed_check'; END IF;
  IF NOT NEW.active OR NOT EXISTS(SELECT 1 FROM document_definitions d WHERE d.id=NEW.document_id AND d.institution_id=NEW.institution_id AND d.active) THEN
   RAISE EXCEPTION 'Document request is inactive' USING ERRCODE='23514',CONSTRAINT='enrollment_document_request_inactive_check';
  END IF;
 ELSIF parent_status <> 'DRAFT' OR NEW.origin <> 'ORIGINAL' THEN
  RAISE EXCEPTION 'Enrollment snapshot is frozen' USING ERRCODE='23514',CONSTRAINT='enrollment_requirement_frozen_check';
 END IF;
 IF TG_OP='UPDATE' AND (NEW.application_id IS DISTINCT FROM OLD.application_id OR NEW.source_requirement_id IS DISTINCT FROM OLD.source_requirement_id
 OR NEW.document_id IS DISTINCT FROM OLD.document_id OR NEW.institution_id IS DISTINCT FROM OLD.institution_id OR NEW.origin IS DISTINCT FROM OLD.origin OR NEW.request_id IS DISTINCT FROM OLD.request_id) THEN
  RAISE EXCEPTION 'Enrollment requirement identity is immutable' USING ERRCODE='23514',CONSTRAINT='enrollment_requirement_identity_check';
 END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION validate_enrollment_document_gate() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.status NOT IN ('SUBMITTED','PROVISIONALLY_APPROVED','APPROVED') OR (TG_OP='UPDATE' AND OLD.status=NEW.status) THEN RETURN NEW; END IF;
 IF EXISTS(SELECT 1 FROM enrollment_document_requirements r WHERE r.application_id=NEW.enrollment_application_id AND r.active
 AND ((NEW.status='SUBMITTED' AND r.origin='ORIGINAL' AND r.level='AT_SUBMISSION')
 OR (NEW.status='PROVISIONALLY_APPROVED' AND r.level='AT_SUBMISSION') OR (NEW.status='APPROVED' AND r.level<>'OPTIONAL'))
 AND NOT EXISTS(SELECT 1 FROM enrollment_attachments f WHERE f.requirement_id=r.id AND f.version_status='CURRENT' AND f.deleted_at IS NULL
 AND r.allowed_formats ? f.content_type AND (NEW.status='SUBMITTED' OR f.review_status='ACCEPTED'))) THEN
 RAISE EXCEPTION 'Required documentation incomplete' USING ERRCODE='23514',CONSTRAINT='enrollment_document_gate_check'; END IF;
 RETURN NEW;
END $$;
-- Pending additional requirements block future approval, never the first partial upload/review.
CREATE OR REPLACE FUNCTION check_enrollment_document_delivery_gate() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent enrollment_applications%ROWTYPE;
BEGIN
 SELECT * INTO parent FROM enrollment_applications WHERE enrollment_application_id=NEW.enrollment_application_id;
 IF parent.status IN ('SUBMITTED','PROVISIONALLY_APPROVED','APPROVED') AND EXISTS(
 SELECT 1 FROM enrollment_document_requirements r WHERE r.application_id=parent.enrollment_application_id AND r.active
 AND ((r.origin='ORIGINAL' AND r.level='AT_SUBMISSION') OR (parent.status='APPROVED' AND r.level<>'OPTIONAL'))
 AND NOT EXISTS(SELECT 1 FROM enrollment_attachments f WHERE f.requirement_id=r.id AND f.version_status='CURRENT' AND f.deleted_at IS NULL
 AND r.allowed_formats ? f.content_type AND (parent.status='SUBMITTED' OR f.review_status='ACCEPTED'))) THEN
 RAISE EXCEPTION 'Required documentation incomplete' USING ERRCODE='23514',CONSTRAINT='enrollment_document_gate_check'; END IF;
 RETURN NULL;
END $$;
CREATE FUNCTION validate_active_enrollment_delivery() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE requirement enrollment_document_requirements%ROWTYPE;
BEGIN
 SELECT * INTO requirement FROM enrollment_document_requirements WHERE id=NEW.requirement_id;
 IF NOT requirement.active THEN RAISE EXCEPTION 'Requirement is retired' USING ERRCODE='23514',CONSTRAINT='enrollment_requirement_retired_check'; END IF;
 IF NEW.version_status='CURRENT' AND NOT (requirement.allowed_formats ? NEW.content_type) THEN
  RAISE EXCEPTION 'File format is not allowed' USING ERRCODE='23514',CONSTRAINT='enrollment_document_format_check';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER enrollment_attachment_requirement_active BEFORE INSERT OR UPDATE ON enrollment_attachments FOR EACH ROW EXECUTE FUNCTION validate_active_enrollment_delivery();

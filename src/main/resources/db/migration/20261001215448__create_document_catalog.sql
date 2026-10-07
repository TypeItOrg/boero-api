CREATE TABLE document_definitions (
 id uuid PRIMARY KEY, institution_id uuid NOT NULL REFERENCES institutions(institution_id),
 name varchar(150) NOT NULL CHECK(length(trim(name))>0), instructions varchar(1000) NOT NULL,
 allowed_formats jsonb NOT NULL CHECK(jsonb_typeof(allowed_formats)='array' AND jsonb_array_length(allowed_formats)>0 AND allowed_formats <@ '["application/pdf","image/jpeg","image/png"]'::jsonb),
 active boolean NOT NULL, revision bigint NOT NULL DEFAULT 0, UNIQUE(institution_id,id)
);
CREATE INDEX document_definitions_institution_idx ON document_definitions(institution_id,active,name,id);
-- Exact matches only; if any path repeats inside a group, preserve every definition separately.
CREATE TEMP TABLE document_catalog_mapping ON COMMIT DROP AS
WITH normalized AS (
 SELECT r.*,p.institution_id,(SELECT jsonb_agg(f ORDER BY f) FROM (SELECT DISTINCT jsonb_array_elements_text(r.allowed_formats) f) formats) formats
 FROM training_path_document_requirements r JOIN training_paths p USING(training_path_id)
), counted AS (
 SELECT *,count(*) OVER(PARTITION BY institution_id,name,instructions,formats,training_path_id) path_count FROM normalized
), mapped AS (
 SELECT *,max(path_count) OVER(PARTITION BY institution_id,name,instructions,formats) duplicate_count,
 first_value(id) OVER(PARTITION BY institution_id,name,instructions,formats ORDER BY id) first_id FROM counted
)
SELECT *,CASE WHEN duplicate_count>1 THEN id ELSE first_id END document_id FROM mapped;
INSERT INTO document_definitions(id,institution_id,name,instructions,allowed_formats,active)
SELECT document_id,institution_id,name,instructions,formats,bool_or(active) FROM document_catalog_mapping
GROUP BY document_id,institution_id,name,instructions,formats;
ALTER TABLE training_path_document_requirements ADD COLUMN document_id uuid, ADD COLUMN institution_id uuid,
 ADD COLUMN specific_instructions varchar(1000), ADD COLUMN revision bigint NOT NULL DEFAULT 0;
UPDATE training_path_document_requirements r SET document_id=m.document_id,institution_id=m.institution_id FROM document_catalog_mapping m WHERE m.id=r.id;
ALTER TABLE training_path_document_requirements ALTER COLUMN document_id SET NOT NULL, ALTER COLUMN institution_id SET NOT NULL,
 ADD CONSTRAINT training_path_document_unique UNIQUE(training_path_id,document_id),
 ADD CONSTRAINT training_path_document_tenant_fk FOREIGN KEY(institution_id,document_id) REFERENCES document_definitions(institution_id,id),
 ADD CONSTRAINT training_path_document_path_tenant_fk FOREIGN KEY(institution_id,training_path_id) REFERENCES training_paths(institution_id,training_path_id),
 DROP COLUMN name,DROP COLUMN instructions,DROP COLUMN allowed_formats;
-- Assignment identity stays stable; replacing a document requires a different assignment.
CREATE FUNCTION protect_training_path_document_assignment() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.document_id IS DISTINCT FROM OLD.document_id OR NEW.training_path_id IS DISTINCT FROM OLD.training_path_id OR NEW.institution_id IS DISTINCT FROM OLD.institution_id THEN
  RAISE EXCEPTION 'Document assignment identity is immutable' USING ERRCODE='23514',CONSTRAINT='training_path_document_identity_check';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER training_path_document_assignment_identity BEFORE UPDATE ON training_path_document_requirements FOR EACH ROW EXECUTE FUNCTION protect_training_path_document_assignment();
ALTER TABLE enrollment_document_requirements ADD COLUMN document_id uuid,ADD COLUMN institution_id uuid,
 ADD COLUMN definition_revision bigint, ADD COLUMN assignment_revision bigint, ADD COLUMN specific_instructions varchar(1000),
 ADD COLUMN active boolean NOT NULL DEFAULT true;
UPDATE enrollment_document_requirements r SET document_id=m.document_id,institution_id=a.institution_id
FROM document_catalog_mapping m,enrollment_applications a WHERE r.source_requirement_id=m.id AND r.application_id=a.enrollment_application_id;
-- A historic source may no longer exist. Give that snapshot its own inactive definition; never rewrite it.
INSERT INTO document_definitions(id,institution_id,name,instructions,allowed_formats,active)
SELECT r.id,a.institution_id,r.name,r.instructions,r.allowed_formats,false FROM enrollment_document_requirements r
JOIN enrollment_applications a ON a.enrollment_application_id=r.application_id WHERE r.document_id IS NULL;
UPDATE enrollment_document_requirements r SET document_id=r.id,institution_id=a.institution_id
FROM enrollment_applications a WHERE a.enrollment_application_id=r.application_id AND r.document_id IS NULL;
ALTER TABLE enrollment_document_requirements ALTER COLUMN document_id SET NOT NULL,ALTER COLUMN institution_id SET NOT NULL,
 ADD CONSTRAINT enrollment_document_tenant_fk FOREIGN KEY(institution_id,document_id) REFERENCES document_definitions(institution_id,id),
 ADD CONSTRAINT enrollment_document_application_tenant_fk FOREIGN KEY(institution_id,application_id) REFERENCES enrollment_applications(institution_id,enrollment_application_id),
 ADD CONSTRAINT enrollment_document_definition_unique UNIQUE(application_id,document_id);
CREATE TABLE enrollment_requirement_changes (
 id uuid PRIMARY KEY,requirement_id uuid NOT NULL REFERENCES enrollment_document_requirements(id),
 action varchar(20) NOT NULL CHECK(action IN ('ADDED','UPDATED','RETIRED','REACTIVATED')),
 occurred_at timestamptz NOT NULL,actor_id uuid,account_type varchar(20) NOT NULL,name varchar(150) NOT NULL
);
CREATE INDEX enrollment_requirement_changes_requirement_idx ON enrollment_requirement_changes(requirement_id,occurred_at,id);
-- Record before reconciling drafts; sent snapshots are deliberately untouched.
INSERT INTO enrollment_requirement_changes SELECT gen_random_uuid(),r.id,
 CASE WHEN NOT COALESCE(s.active AND d.active,false) THEN 'RETIRED' ELSE 'UPDATED' END,now(),NULL,'MIGRATION',r.name
FROM enrollment_document_requirements r JOIN enrollment_applications a ON a.enrollment_application_id=r.application_id
LEFT JOIN training_path_document_requirements s ON s.id=r.source_requirement_id
LEFT JOIN document_definitions d ON d.id=s.document_id
WHERE a.status='DRAFT' AND a.deleted_at IS NULL AND (NOT COALESCE(s.active AND d.active,false)
 OR r.name IS DISTINCT FROM d.name OR r.instructions IS DISTINCT FROM d.instructions OR r.allowed_formats IS DISTINCT FROM d.allowed_formats
 OR r.level IS DISTINCT FROM s.level OR r.display_order IS DISTINCT FROM s.display_order);
UPDATE enrollment_document_requirements r SET active=false FROM enrollment_applications a
WHERE a.enrollment_application_id=r.application_id AND a.status='DRAFT' AND a.deleted_at IS NULL
AND NOT EXISTS(SELECT 1 FROM training_path_document_requirements s JOIN document_definitions d ON d.id=s.document_id WHERE s.id=r.source_requirement_id AND s.active AND d.active);
UPDATE enrollment_document_requirements r SET name=d.name,instructions=d.instructions,allowed_formats=d.allowed_formats,
 level=s.level,display_order=s.display_order,specific_instructions=s.specific_instructions,definition_revision=d.revision,assignment_revision=s.revision,active=true
FROM enrollment_applications a,training_path_document_requirements s,document_definitions d
WHERE a.enrollment_application_id=r.application_id AND a.status='DRAFT' AND a.deleted_at IS NULL
AND s.id=r.source_requirement_id AND d.id=s.document_id AND s.active AND d.active;
WITH added AS (
 INSERT INTO enrollment_document_requirements(id,application_id,source_requirement_id,name,instructions,level,allowed_formats,display_order,document_id,institution_id,definition_revision,assignment_revision)
 SELECT gen_random_uuid(),a.enrollment_application_id,s.id,d.name,d.instructions,s.level,d.allowed_formats,s.display_order,d.id,a.institution_id,d.revision,s.revision
 FROM enrollment_applications a JOIN training_path_document_requirements s ON s.training_path_id=a.training_path_id
 JOIN document_definitions d ON d.id=s.document_id WHERE a.status='DRAFT' AND a.deleted_at IS NULL AND s.active AND d.active
 AND NOT EXISTS(SELECT 1 FROM enrollment_document_requirements r WHERE r.application_id=a.enrollment_application_id AND r.source_requirement_id=s.id)
 RETURNING id,name
) INSERT INTO enrollment_requirement_changes SELECT gen_random_uuid(),id,'ADDED',now(),NULL,'MIGRATION',name FROM added;
-- Lifecycle state cannot be changed by an incidental snapshot edit after submission.
CREATE FUNCTION protect_enrollment_requirement_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_status varchar(40);
BEGIN
 SELECT status INTO parent_status FROM enrollment_applications WHERE enrollment_application_id=NEW.application_id FOR UPDATE;
 IF parent_status <> 'DRAFT' THEN
  RAISE EXCEPTION 'Enrollment snapshot is frozen' USING ERRCODE='23514',CONSTRAINT='enrollment_requirement_frozen_check';
 END IF;
 IF TG_OP='UPDATE' AND (NEW.application_id IS DISTINCT FROM OLD.application_id OR NEW.source_requirement_id IS DISTINCT FROM OLD.source_requirement_id OR NEW.document_id IS DISTINCT FROM OLD.document_id OR NEW.institution_id IS DISTINCT FROM OLD.institution_id) THEN
  RAISE EXCEPTION 'Enrollment requirement identity is immutable' USING ERRCODE='23514',CONSTRAINT='enrollment_requirement_identity_check';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER enrollment_requirement_snapshot BEFORE INSERT OR UPDATE ON enrollment_document_requirements FOR EACH ROW EXECUTE FUNCTION protect_enrollment_requirement_snapshot();
CREATE OR REPLACE FUNCTION validate_enrollment_document_gate() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.status NOT IN ('SUBMITTED','PROVISIONALLY_APPROVED','APPROVED') OR (TG_OP='UPDATE' AND OLD.status=NEW.status) THEN RETURN NEW; END IF;
 IF EXISTS(SELECT 1 FROM enrollment_document_requirements r WHERE r.application_id=NEW.enrollment_application_id AND r.active
 AND (r.level='AT_SUBMISSION' OR NEW.status='APPROVED' AND r.level='BEFORE_CONFIRMATION')
 AND NOT EXISTS(SELECT 1 FROM enrollment_attachments f WHERE f.requirement_id=r.id AND f.version_status='CURRENT' AND f.deleted_at IS NULL
 AND r.allowed_formats ? f.content_type AND (NEW.status='SUBMITTED' OR f.review_status='ACCEPTED'))) THEN
 RAISE EXCEPTION 'Required documentation incomplete' USING ERRCODE='23514',CONSTRAINT='enrollment_document_gate_check'; END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION check_enrollment_document_delivery_gate() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent enrollment_applications%ROWTYPE;
BEGIN
 SELECT * INTO parent FROM enrollment_applications WHERE enrollment_application_id=NEW.enrollment_application_id;
 IF parent.status IN ('SUBMITTED','PROVISIONALLY_APPROVED','APPROVED') AND EXISTS(
 SELECT 1 FROM enrollment_document_requirements r WHERE r.application_id=parent.enrollment_application_id AND r.active
 AND (r.level='AT_SUBMISSION' OR parent.status='APPROVED' AND r.level='BEFORE_CONFIRMATION')
 AND NOT EXISTS(SELECT 1 FROM enrollment_attachments f WHERE f.requirement_id=r.id AND f.version_status='CURRENT' AND f.deleted_at IS NULL
 AND r.allowed_formats ? f.content_type AND (parent.status='SUBMITTED' OR f.review_status='ACCEPTED'))) THEN
 RAISE EXCEPTION 'Required documentation incomplete' USING ERRCODE='23514',CONSTRAINT='enrollment_document_gate_check'; END IF;
 RETURN NULL;
END $$;

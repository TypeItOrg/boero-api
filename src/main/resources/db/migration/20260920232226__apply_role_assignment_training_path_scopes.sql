ALTER TABLE person_role_assignments
    ADD COLUMN access_scope varchar(20) NOT NULL DEFAULT 'INSTITUTION',
    ADD CONSTRAINT person_role_assignments_access_scope_check
        CHECK (access_scope IN ('INSTITUTION', 'TRAINING_PATHS'));

CREATE TABLE person_role_assignment_training_paths (
    person_role_assignment_id uuid NOT NULL REFERENCES person_role_assignments ON DELETE CASCADE,
    training_path_id uuid NOT NULL REFERENCES training_paths,
    PRIMARY KEY (person_role_assignment_id, training_path_id)
);
CREATE INDEX person_role_assignment_training_paths_path_idx
    ON person_role_assignment_training_paths (training_path_id);

-- Deferred validation allows Hibernate to replace the collection within one transaction.
-- Locking the assignment serializes concurrent collection changes.
CREATE FUNCTION validate_person_role_assignment_scope() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    assignment_id uuid;
    assignment_scope varchar;
    tenant_id uuid;
    path_count bigint;
BEGIN
    IF TG_OP = 'DELETE' THEN
        assignment_id := OLD.person_role_assignment_id;
    ELSE
        assignment_id := NEW.person_role_assignment_id;
    END IF;
    SELECT access_scope, institution_id INTO assignment_scope, tenant_id
    FROM person_role_assignments WHERE person_role_assignment_id = assignment_id FOR UPDATE;
    IF NOT FOUND THEN RETURN NULL; END IF;
    SELECT count(*) INTO path_count FROM person_role_assignment_training_paths
    WHERE person_role_assignment_id = assignment_id;
    IF (assignment_scope = 'INSTITUTION' AND path_count <> 0)
        OR (assignment_scope = 'TRAINING_PATHS' AND path_count = 0) THEN
        RAISE EXCEPTION 'Invalid role assignment scope' USING ERRCODE = '23514',
            CONSTRAINT = 'person_role_assignment_scope_consistency';
    END IF;
    IF EXISTS (
        SELECT 1 FROM person_role_assignment_training_paths selected
        JOIN training_paths path USING (training_path_id)
        WHERE selected.person_role_assignment_id = assignment_id AND path.institution_id <> tenant_id
    ) THEN
        RAISE EXCEPTION 'Training path belongs to another institution' USING ERRCODE = '23514',
            CONSTRAINT = 'person_role_assignment_scope_consistency';
    END IF;
    RETURN NULL;
END;
$$;
CREATE CONSTRAINT TRIGGER person_role_assignment_scope_check
AFTER INSERT OR UPDATE ON person_role_assignments DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_person_role_assignment_scope();
CREATE CONSTRAINT TRIGGER person_role_assignment_paths_check
AFTER INSERT OR UPDATE OR DELETE ON person_role_assignment_training_paths DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_person_role_assignment_scope();

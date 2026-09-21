ALTER TABLE person_role_assignments ADD CONSTRAINT person_role_assignments_tenant_id_unique
    UNIQUE (institution_id, person_role_assignment_id);
ALTER TABLE person_role_assignment_training_paths ADD COLUMN institution_id uuid;
UPDATE person_role_assignment_training_paths selected SET institution_id = assignment.institution_id
FROM person_role_assignments assignment WHERE assignment.person_role_assignment_id = selected.person_role_assignment_id;
ALTER TABLE person_role_assignment_training_paths ALTER COLUMN institution_id SET NOT NULL;
ALTER TABLE person_role_assignment_training_paths
    ADD CONSTRAINT role_scope_assignment_tenant_fk FOREIGN KEY (institution_id, person_role_assignment_id)
        REFERENCES person_role_assignments (institution_id, person_role_assignment_id) ON DELETE CASCADE,
    ADD CONSTRAINT role_scope_training_path_tenant_fk FOREIGN KEY (institution_id, training_path_id)
        REFERENCES training_paths (institution_id, training_path_id);

CREATE FUNCTION prepare_role_scope_training_path() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.person_role_assignment_id <> OLD.person_role_assignment_id THEN
        RAISE EXCEPTION 'Assignment scope relations cannot be transferred' USING ERRCODE = '23514',
            CONSTRAINT = 'person_role_assignment_scope_consistency';
    END IF;
    SELECT institution_id INTO NEW.institution_id FROM person_role_assignments
    WHERE person_role_assignment_id = NEW.person_role_assignment_id FOR UPDATE;
    RETURN NEW;
END;
$$;
CREATE TRIGGER prepare_role_scope_training_path BEFORE INSERT OR UPDATE ON person_role_assignment_training_paths
FOR EACH ROW EXECUTE FUNCTION prepare_role_scope_training_path();

CREATE FUNCTION enforce_institutional_authority_scope() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.access_scope <> 'INSTITUTION' AND EXISTS (
        SELECT 1 FROM roles WHERE role_id = NEW.role_id AND is_system = true AND code = 'INSTITUTIONAL_AUTHORITY'
    ) THEN
        RAISE EXCEPTION 'Institutional authority requires institution scope' USING ERRCODE = '23514',
            CONSTRAINT = 'person_role_assignment_scope_consistency';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER enforce_institutional_authority_scope BEFORE INSERT OR UPDATE ON person_role_assignments
FOR EACH ROW EXECUTE FUNCTION enforce_institutional_authority_scope();

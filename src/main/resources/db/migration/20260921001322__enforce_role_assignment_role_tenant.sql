ALTER TABLE roles
    ADD CONSTRAINT roles_institution_id_role_id_unique UNIQUE (institution_id, role_id);

ALTER TABLE person_role_assignments
    ADD CONSTRAINT person_role_assignments_role_institution_fk
        FOREIGN KEY (institution_id, role_id)
        REFERENCES roles (institution_id, role_id);

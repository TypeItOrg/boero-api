CREATE UNIQUE INDEX roles_institution_name_lower_unique
    ON roles (institution_id, LOWER(name))
    WHERE institution_id IS NOT NULL;

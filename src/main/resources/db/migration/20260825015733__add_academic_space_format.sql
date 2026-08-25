ALTER TABLE academic_spaces ADD COLUMN format varchar(20);

UPDATE academic_spaces SET format = 'INDIVIDUAL';

ALTER TABLE academic_spaces ALTER COLUMN format SET NOT NULL;

ALTER TABLE academic_spaces
    ADD CONSTRAINT academic_spaces_format_check CHECK (format IN ('INDIVIDUAL', 'GRUPAL'));

DROP INDEX academic_spaces_current_institution_name_type_unique;

CREATE UNIQUE INDEX academic_spaces_current_institution_name_type_format_unique
    ON academic_spaces (
        institution_id,
        lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')),
        type,
        format
    )
    WHERE deleted_at IS NULL;

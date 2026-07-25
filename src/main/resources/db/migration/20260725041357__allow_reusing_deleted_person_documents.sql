ALTER TABLE people DROP CONSTRAINT people_document_number_unique;

CREATE UNIQUE INDEX people_active_document_number_unique
    ON people (institution_id, document_number)
    WHERE deleted = false;

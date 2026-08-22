UPDATE people
SET email = lower(document_number) || '@pendiente.invalid'
WHERE email IS NULL OR btrim(email) = '';

ALTER TABLE people
    ALTER COLUMN email SET NOT NULL;

ALTER TABLE people
    ADD CONSTRAINT people_email_not_blank CHECK (btrim(email) <> '');

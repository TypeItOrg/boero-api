-- Created with `make migration add_shifts`.
CREATE TABLE shifts (
    shift_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    name varchar(150) NOT NULL,
    description varchar(1000),
    active boolean NOT NULL,
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    deleted_at timestamp(6),
    CONSTRAINT shifts_pkey PRIMARY KEY (shift_id),
    CONSTRAINT shifts_institution_id_id_unique UNIQUE (institution_id, shift_id),
    CONSTRAINT shifts_name_format_check CHECK (name = regexp_replace(btrim(name), '[[:space:]]+', ' ', 'g')),
    CONSTRAINT shifts_deleted_state_check CHECK (deleted_at IS NULL OR active = false),
    CONSTRAINT shifts_institution_fk FOREIGN KEY (institution_id) REFERENCES institutions (institution_id)
);

CREATE UNIQUE INDEX shifts_current_institution_name_unique
    ON shifts (
        institution_id,
        lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))
    )
    WHERE deleted_at IS NULL;

CREATE INDEX shifts_institution_deleted_at_idx
    ON shifts (institution_id, deleted_at);

ALTER TABLE academic_lifecycle_events DROP CONSTRAINT academic_lifecycle_events_resource_type_check;

ALTER TABLE academic_lifecycle_events
    ADD CONSTRAINT academic_lifecycle_events_resource_type_check
    CHECK (resource_type IN ('ACADEMIC_YEAR', 'TRAINING_PATH', 'STUDY_PLAN', 'ACADEMIC_SPACE', 'INSTRUMENT', 'SHIFT'));

INSERT INTO permissions (created_at, updated_at, permission_id, scope, code, description)
VALUES
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:shift:read')::uuid, 'INSTITUTION', 'institution:shift:read', 'Ver turnos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:shift:create')::uuid, 'INSTITUTION', 'institution:shift:create', 'Crear turnos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:shift:update')::uuid, 'INSTITUTION', 'institution:shift:update', 'Editar turnos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:shift:update-status')::uuid, 'INSTITUTION', 'institution:shift:update-status', 'Activar o desactivar turnos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:shift:delete')::uuid, 'INSTITUTION', 'institution:shift:delete', 'Eliminar turnos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:shift:restore')::uuid, 'INSTITUTION', 'institution:shift:restore', 'Restaurar turnos')
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    scope = EXCLUDED.scope,
    updated_at = CURRENT_TIMESTAMP;

WITH permission_mappings(source_code, target_code) AS (
    VALUES
        ('institution:instrument:create', 'institution:shift:create'),
        ('institution:instrument:update', 'institution:shift:update'),
        ('institution:instrument:update-status', 'institution:shift:update-status'),
        ('institution:instrument:delete', 'institution:shift:delete'),
        ('institution:instrument:restore', 'institution:shift:restore')
)
INSERT INTO role_permissions (permission_id, role_id)
SELECT target_permission.permission_id, role_permission.role_id
FROM role_permissions role_permission
JOIN permissions source_permission
    ON source_permission.permission_id = role_permission.permission_id
JOIN permission_mappings
    ON permission_mappings.source_code = source_permission.code
JOIN permissions target_permission
    ON target_permission.code = permission_mappings.target_code
ON CONFLICT (permission_id, role_id) DO NOTHING;

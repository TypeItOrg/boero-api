-- Created with `make migration add_academic_soft_delete`.
ALTER TABLE academic_years ADD COLUMN deleted_at timestamp(6);
ALTER TABLE training_paths ADD COLUMN deleted_at timestamp(6);
ALTER TABLE study_plans ADD COLUMN deleted_at timestamp(6);
ALTER TABLE academic_spaces ADD COLUMN deleted_at timestamp(6);
ALTER TABLE instruments ADD COLUMN deleted_at timestamp(6);

ALTER TABLE academic_years
    ADD CONSTRAINT academic_years_deleted_state_check
    CHECK (deleted_at IS NULL OR status = 'PLANNED');

ALTER TABLE training_paths
    ADD CONSTRAINT training_paths_deleted_state_check
    CHECK (deleted_at IS NULL OR active = false);

ALTER TABLE study_plans
    ADD CONSTRAINT study_plans_deleted_state_check
    CHECK (deleted_at IS NULL OR status = 'DRAFT');

ALTER TABLE academic_spaces
    ADD CONSTRAINT academic_spaces_deleted_state_check
    CHECK (deleted_at IS NULL OR active = false);

ALTER TABLE instruments
    ADD CONSTRAINT instruments_deleted_state_check
    CHECK (deleted_at IS NULL OR active = false);

ALTER TABLE academic_years DROP CONSTRAINT academic_years_institution_year_unique;
DROP INDEX academic_years_active_institution_unique;
DROP INDEX training_paths_institution_name_unique;
DROP INDEX study_plans_institution_name_unique;
DROP INDEX academic_spaces_institution_name_type_unique;
DROP INDEX instruments_institution_name_unique;

CREATE UNIQUE INDEX academic_years_current_institution_year_unique
    ON academic_years (institution_id, year)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX academic_years_active_institution_unique
    ON academic_years (institution_id)
    WHERE status = 'ACTIVE' AND deleted_at IS NULL;

CREATE UNIQUE INDEX training_paths_current_institution_name_unique
    ON training_paths (
        institution_id,
        lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))
    )
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX study_plans_current_training_path_name_unique
    ON study_plans (
        training_path_id,
        lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))
    )
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX academic_spaces_current_institution_name_type_unique
    ON academic_spaces (
        institution_id,
        lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')),
        type
    )
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX instruments_current_institution_name_unique
    ON instruments (
        institution_id,
        lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'))
    )
    WHERE deleted_at IS NULL;

CREATE INDEX academic_years_institution_deleted_at_idx
    ON academic_years (institution_id, deleted_at);
CREATE INDEX training_paths_institution_deleted_at_idx
    ON training_paths (institution_id, deleted_at);
CREATE INDEX study_plans_institution_deleted_at_idx
    ON study_plans (institution_id, deleted_at);
CREATE INDEX academic_spaces_institution_deleted_at_idx
    ON academic_spaces (institution_id, deleted_at);
CREATE INDEX instruments_institution_deleted_at_idx
    ON instruments (institution_id, deleted_at);

CREATE TABLE academic_lifecycle_events (
    academic_lifecycle_event_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    resource_type varchar(30) NOT NULL,
    resource_id uuid NOT NULL,
    action varchar(20) NOT NULL,
    actor_type varchar(20) NOT NULL,
    actor_id uuid NOT NULL,
    reason varchar(500),
    request_id varchar(36),
    created_at timestamp(6) NOT NULL,
    CONSTRAINT academic_lifecycle_events_pkey PRIMARY KEY (academic_lifecycle_event_id),
    CONSTRAINT academic_lifecycle_events_institution_fk
        FOREIGN KEY (institution_id) REFERENCES institutions (institution_id),
    CONSTRAINT academic_lifecycle_events_resource_type_check
        CHECK (resource_type IN ('ACADEMIC_YEAR', 'TRAINING_PATH', 'STUDY_PLAN', 'ACADEMIC_SPACE', 'INSTRUMENT')),
    CONSTRAINT academic_lifecycle_events_action_check CHECK (action IN ('DELETE', 'RESTORE')),
    CONSTRAINT academic_lifecycle_events_actor_type_check CHECK (actor_type IN ('INSTITUTION', 'PLATFORM'))
);

CREATE INDEX academic_lifecycle_events_resource_idx
    ON academic_lifecycle_events (institution_id, resource_type, resource_id, created_at DESC);

INSERT INTO permissions (created_at, updated_at, permission_id, scope, code, description)
VALUES
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-year:delete')::uuid, 'INSTITUTION', 'institution:academic-year:delete', 'Eliminar ciclos lectivos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-year:restore')::uuid, 'INSTITUTION', 'institution:academic-year:restore', 'Restaurar ciclos lectivos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:training-path:delete')::uuid, 'INSTITUTION', 'institution:training-path:delete', 'Eliminar trayectos formativos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:training-path:restore')::uuid, 'INSTITUTION', 'institution:training-path:restore', 'Restaurar trayectos formativos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:study-plan:delete')::uuid, 'INSTITUTION', 'institution:study-plan:delete', 'Eliminar planes de estudio'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:study-plan:restore')::uuid, 'INSTITUTION', 'institution:study-plan:restore', 'Restaurar planes de estudio'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-space:delete')::uuid, 'INSTITUTION', 'institution:academic-space:delete', 'Eliminar espacios académicos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:academic-space:restore')::uuid, 'INSTITUTION', 'institution:academic-space:restore', 'Restaurar espacios académicos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:instrument:delete')::uuid, 'INSTITUTION', 'institution:instrument:delete', 'Eliminar instrumentos'),
    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, md5('institution:instrument:restore')::uuid, 'INSTITUTION', 'institution:instrument:restore', 'Restaurar instrumentos')
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description,
    scope = EXCLUDED.scope,
    updated_at = CURRENT_TIMESTAMP;

WITH permission_mappings(source_code, target_code) AS (
    VALUES
        ('institution:academic-year:update', 'institution:academic-year:delete'),
        ('institution:academic-year:update', 'institution:academic-year:restore'),
        ('institution:training-path:update', 'institution:training-path:delete'),
        ('institution:training-path:update', 'institution:training-path:restore'),
        ('institution:study-plan:update', 'institution:study-plan:delete'),
        ('institution:study-plan:update', 'institution:study-plan:restore'),
        ('institution:academic-space:update', 'institution:academic-space:delete'),
        ('institution:academic-space:update', 'institution:academic-space:restore'),
        ('institution:instrument:update', 'institution:instrument:delete'),
        ('institution:instrument:update', 'institution:instrument:restore')
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

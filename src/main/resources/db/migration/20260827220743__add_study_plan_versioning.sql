ALTER TABLE study_plans ADD COLUMN previous_version_id uuid;
ALTER TABLE study_plans ADD COLUMN version_number integer NOT NULL DEFAULT 1;

ALTER TABLE study_plans
    ADD CONSTRAINT study_plans_version_number_check
    CHECK (version_number > 0);

ALTER TABLE study_plans
    ADD CONSTRAINT study_plans_previous_version_self_check
    CHECK (previous_version_id IS NULL OR previous_version_id <> study_plan_id);

ALTER TABLE study_plans
    ADD CONSTRAINT study_plans_previous_version_institution_fk
    FOREIGN KEY (institution_id, previous_version_id)
    REFERENCES study_plans (institution_id, study_plan_id);

CREATE UNIQUE INDEX study_plans_current_successor_unique
    ON study_plans (previous_version_id)
    WHERE previous_version_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX study_plans_previous_version_idx
    ON study_plans (previous_version_id);

DROP INDEX study_plans_current_training_path_name_unique;
CREATE UNIQUE INDEX study_plans_current_training_path_name_version_unique
    ON study_plans (
        training_path_id,
        lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')),
        version_number
    )
    WHERE deleted_at IS NULL;

ALTER TABLE study_plans DROP CONSTRAINT study_plans_deleted_state_check;
ALTER TABLE study_plans
    ADD CONSTRAINT study_plans_deleted_state_check
    CHECK (deleted_at IS NULL OR status IN ('DRAFT', 'INACTIVE'));

ALTER TABLE academic_lifecycle_events DROP CONSTRAINT academic_lifecycle_events_action_check;
ALTER TABLE academic_lifecycle_events
    ADD CONSTRAINT academic_lifecycle_events_action_check
    CHECK (action IN ('DELETE', 'RESTORE', 'CREATE_VERSION'));

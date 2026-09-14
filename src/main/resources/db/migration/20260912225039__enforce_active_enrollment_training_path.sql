-- Keep the indexed path consistent with the selected plan, including direct SQL writes.
ALTER TABLE study_plans
    ADD CONSTRAINT study_plans_id_training_path_unique UNIQUE (study_plan_id, training_path_id);

ALTER TABLE enrollment_applications ADD COLUMN training_path_id uuid;

UPDATE enrollment_applications application
SET training_path_id = plan.training_path_id
FROM study_plans plan
WHERE plan.study_plan_id = application.study_plan_id;

ALTER TABLE enrollment_applications
    ALTER COLUMN training_path_id SET NOT NULL,
    ADD CONSTRAINT enrollment_apps_plan_training_path_fk
        FOREIGN KEY (study_plan_id, training_path_id)
        REFERENCES study_plans (study_plan_id, training_path_id);

-- Existing duplicates require business review; never discard applications during migration.
CREATE UNIQUE INDEX enrollment_apps_applicant_active_path_unique
    ON enrollment_applications (applicant_person_id, training_path_id)
    WHERE deleted_at IS NULL AND status NOT IN ('CANCELLED', 'REJECTED');

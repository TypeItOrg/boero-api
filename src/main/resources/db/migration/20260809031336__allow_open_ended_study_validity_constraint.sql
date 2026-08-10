ALTER TABLE study_plans
    DROP CONSTRAINT study_plans_dates_pair_check,
    ADD CONSTRAINT study_plans_end_requires_start_check
        CHECK (effective_to IS NULL OR effective_from IS NOT NULL);

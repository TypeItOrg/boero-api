ALTER TABLE study_plans
    ADD CONSTRAINT study_plans_dates_pair_check
    CHECK ((effective_from IS NULL) = (effective_to IS NULL));

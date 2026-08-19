CREATE INDEX IF NOT EXISTS study_plan_spaces_academic_space_plan_idx
    ON study_plan_spaces (academic_space_id, study_plan_id);

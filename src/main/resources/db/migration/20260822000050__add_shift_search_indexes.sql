-- Created with `make migration add_shift_search_indexes`.
CREATE INDEX shifts_contextual_search_idx
    ON shifts USING gin (boero_search_vector(name));

CREATE INDEX shifts_name_search_trgm_idx
    ON shifts USING gin (boero_normalize_text(name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

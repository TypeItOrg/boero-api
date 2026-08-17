-- Created with `make migration add_partial_search_trigram_indexes`.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX institutions_name_search_trgm_idx
    ON institutions USING gin (boero_normalize_text(name) gin_trgm_ops);

CREATE INDEX institutions_slug_search_trgm_idx
    ON institutions USING gin (boero_normalize_text(slug) gin_trgm_ops);

CREATE INDEX people_full_name_search_trgm_idx
    ON people USING gin (boero_normalize_text(first_name || ' ' || last_name) gin_trgm_ops)
    WHERE NOT deleted;

CREATE INDEX people_document_search_trgm_idx
    ON people USING gin (document_number gin_trgm_ops)
    WHERE NOT deleted;

CREATE INDEX people_email_search_trgm_idx
    ON people USING gin (boero_normalize_text(email) gin_trgm_ops)
    WHERE NOT deleted;

CREATE INDEX roles_name_search_trgm_idx
    ON roles USING gin (boero_normalize_text(name) gin_trgm_ops)
    WHERE scope = 'INSTITUTION';

CREATE INDEX roles_code_search_trgm_idx
    ON roles USING gin (boero_normalize_text(code) gin_trgm_ops)
    WHERE scope = 'INSTITUTION';

CREATE INDEX platform_accounts_full_name_search_trgm_idx
    ON platform_accounts USING gin (boero_normalize_text(first_name || ' ' || last_name) gin_trgm_ops);

CREATE INDEX platform_accounts_email_search_trgm_idx
    ON platform_accounts USING gin (lower(email) gin_trgm_ops);

CREATE INDEX platform_accounts_email_upper_idx
    ON platform_accounts (upper(email));

CREATE INDEX countries_name_search_trgm_idx
    ON countries USING gin (boero_normalize_text(name) gin_trgm_ops);

CREATE INDEX provinces_name_search_trgm_idx
    ON provinces USING gin (boero_normalize_text(name) gin_trgm_ops);

CREATE INDEX cities_name_search_trgm_idx
    ON cities USING gin (boero_normalize_text(name) gin_trgm_ops);

CREATE INDEX training_paths_name_search_trgm_idx
    ON training_paths USING gin (boero_normalize_text(name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX study_plans_name_search_trgm_idx
    ON study_plans USING gin (boero_normalize_text(name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX academic_spaces_name_search_trgm_idx
    ON academic_spaces USING gin (boero_normalize_text(name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX instruments_name_search_trgm_idx
    ON instruments USING gin (boero_normalize_text(name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

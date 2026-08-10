CREATE OR REPLACE FUNCTION boero_normalize_text(value text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
RETURN lower(translate(coalesce(value, ''), 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'));

CREATE OR REPLACE FUNCTION boero_search_vector(value text)
RETURNS tsvector
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
RETURN to_tsvector('simple', boero_normalize_text(value));

CREATE OR REPLACE FUNCTION boero_search_rank(value text, query text, normalized_query text)
RETURNS real
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
RETURN CASE WHEN boero_normalize_text(value) = normalized_query THEN 10 ELSE 0 END
       + ts_rank_cd(boero_search_vector(value), to_tsquery('simple', query));

CREATE INDEX institutions_contextual_search_idx
    ON institutions USING gin (boero_search_vector(name || ' ' || slug));

CREATE INDEX people_contextual_search_idx
    ON people USING gin (boero_search_vector(first_name || ' ' || last_name || ' ' || last_name || ' ' || first_name || ' ' || document_number || ' ' || coalesce(email, '')))
    WHERE NOT deleted;

CREATE INDEX roles_contextual_search_idx
    ON roles USING gin (boero_search_vector(name || ' ' || code))
    WHERE scope = 'INSTITUTION';

CREATE INDEX platform_accounts_contextual_search_idx
    ON platform_accounts USING gin (boero_search_vector(first_name || ' ' || last_name || ' ' || last_name || ' ' || first_name || ' ' || email));

CREATE INDEX academic_years_contextual_search_idx
    ON academic_years USING gin (boero_search_vector(year::text));

CREATE INDEX training_paths_contextual_search_idx
    ON training_paths USING gin (boero_search_vector(name));

CREATE INDEX study_plans_contextual_search_idx
    ON study_plans USING gin (boero_search_vector(name));

CREATE INDEX academic_spaces_contextual_search_idx
    ON academic_spaces USING gin (boero_search_vector(name));

CREATE INDEX instruments_contextual_search_idx
    ON instruments USING gin (boero_search_vector(name));

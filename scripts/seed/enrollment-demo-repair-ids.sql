-- Explicit, backed-up LOCAL repair. Concatenate after enrollment-demo-ids.sql
-- and enrollment-demo-catalog.sql. Does not delete or recreate business rows.
CREATE TEMP TABLE seed_id_targets (table_name text, column_name text, legacy_id uuid PRIMARY KEY, id uuid UNIQUE) ON COMMIT DROP;
CREATE FUNCTION pg_temp.register_seed_id(target_table text, target_column text, seed_namespace text, key text)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    canonical_key text := pg_temp.seed_key(key);
    old_id uuid := md5(seed_namespace || canonical_key)::uuid;
    present boolean;
    new_id uuid;
BEGIN
    EXECUTE format('SELECT EXISTS (SELECT 1 FROM public.%I WHERE %I = $1)', target_table, target_column)
        INTO present USING old_id;
    IF present THEN
        new_id := pg_temp.seed_id(seed_namespace, canonical_key);
        INSERT INTO seed_id_targets VALUES (target_table, target_column, old_id, new_id) ON CONFLICT DO NOTHING;
    END IF;
END $$;

DO $$
DECLARE
    t record;
    member integer;
    item record;
    demo text := 'boero:enrollment-demo:v1:';
    academic text := 'boero:enrollment:official-2025:';
    tenant uuid := '019e18e4-d919-76d8-9848-7f1b14e64452';
BEGIN
    -- Stable ordering and a bounded timeout prevent concurrent writes during rekeying.
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename LOOP
        EXECUTE format('LOCK TABLE public.%I IN ACCESS EXCLUSIVE MODE', t.tablename);
    END LOOP;
    PERFORM 1 FROM institutions WHERE institution_id = tenant AND active;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La institución local del seed no existe o está inactiva.';
    END IF;
    FOR member IN 1..8 LOOP
        PERFORM pg_temp.register_seed_id('people', 'person_id', demo, 'person:' || member);
        PERFORM pg_temp.register_seed_id('users', 'user_id', demo, 'user:' || member);
        PERFORM pg_temp.register_seed_id('person_role_assignments', 'person_role_assignment_id', demo, 'role:' || member);
        PERFORM pg_temp.register_seed_id('students', 'student_id', demo, 'student:' || member);
    END LOOP;
    FOR item IN SELECT year FROM academic_years WHERE institution_id = tenant LOOP
        PERFORM pg_temp.register_seed_id('academic_years', 'academic_year_id', demo, 'year:' || item.year);
    END LOOP;
    FOR item IN SELECT name FROM instruments WHERE institution_id = tenant LOOP
        PERFORM pg_temp.register_seed_id('instruments', 'instrument_id', academic, 'instrument:' || item.name);
    END LOOP;
    FOR item IN SELECT name FROM shifts WHERE institution_id = tenant LOOP
        PERFORM pg_temp.register_seed_id('shifts', 'shift_id', academic, 'shift:' || item.name);
    END LOOP;
    FOR item IN SELECT code FROM seed_programs LOOP
        PERFORM pg_temp.register_seed_id('training_paths', 'training_path_id', academic, 'path:' || item.code);
        PERFORM pg_temp.register_seed_id('study_plans', 'study_plan_id', academic, 'plan:' || item.code);
    END LOOP;
    FOR item IN SELECT p.code, y.year FROM seed_programs p CROSS JOIN academic_years y WHERE y.institution_id = tenant LOOP
        PERFORM pg_temp.register_seed_id('enrollment_periods', 'enrollment_period_id', academic, 'period:' || item.code || ':' || item.year);
        PERFORM pg_temp.register_seed_id('enrollment_period_offerings', 'offering_id', academic, 'offering:' || item.code || ':' || item.year);
        PERFORM pg_temp.register_seed_id('enrollment_period_offering_levels', 'offering_level_id', academic, 'offering-level:' || item.code || ':' || item.year);
    END LOOP;
    FOR item IN SELECT code, generate_series(1, levels) AS level FROM seed_programs LOOP
        PERFORM pg_temp.register_seed_id('academic_levels', 'academic_level_id', academic, 'level:' || item.code || ':' || item.level);
    END LOOP;
    FOR item IN SELECT * FROM seed_curriculum LOOP
        PERFORM pg_temp.register_seed_id('academic_spaces', 'academic_space_id', academic, 'space:' || item.name || ':' || item.type || ':' || item.format);
        PERFORM pg_temp.register_seed_id('study_plan_spaces', 'study_plan_space_id', academic, 'curriculum:' || item.program || ':' || item.level || ':' || item.position);
    END LOOP;
    FOR item IN SELECT * FROM study_plan_space_instruments WHERE institution_id = tenant LOOP
        PERFORM pg_temp.register_seed_id('study_plan_space_instruments', 'study_plan_space_instrument_id', academic, 'allowed:' || item.study_plan_space_id || ':' || item.instrument_id);
    END LOOP;
    FOR item IN SELECT c.*, y.year FROM courses c JOIN academic_years y USING (academic_year_id) WHERE c.institution_id = tenant LOOP
        PERFORM pg_temp.register_seed_id('courses', 'course_id', academic, 'course:' || item.study_plan_space_id || ':' || coalesce(item.instrument_id::text, 'group') || ':' || item.year);
        PERFORM pg_temp.register_seed_id('course_classes', 'course_class_id', academic, 'class:' || item.course_id);
        PERFORM pg_temp.register_seed_id('course_class_teachers', 'course_class_teacher_id', academic, 'teacher:' || item.course_id);
        PERFORM pg_temp.register_seed_id('course_class_days', 'course_class_day_id', academic, 'day:' || item.course_id);
        PERFORM pg_temp.register_seed_id('course_class_schedules', 'course_class_schedule_id', academic, 'schedule:' || item.course_id);
        PERFORM pg_temp.register_seed_id('course_individual_slots', 'course_individual_slot_id', academic, 'slot:' || item.course_id || ':0');
        PERFORM pg_temp.register_seed_id('course_individual_slots', 'course_individual_slot_id', academic, 'slot:' || item.course_id || ':1');
    END LOOP;
END $$;

-- Abort on non-relational references rather than silently changing file paths,
-- credentials or arbitrary user text. Such references require a specific repair.
DO $$
DECLARE c record; present boolean;
BEGIN
    FOR c IN SELECT table_name, column_name FROM information_schema.columns
        WHERE table_schema = 'public' AND data_type IN ('text', 'character varying', 'json', 'jsonb', 'ARRAY') LOOP
        EXECUTE format('SELECT EXISTS (SELECT 1 FROM public.%I t JOIN seed_id_targets m ON position(m.legacy_id::text IN t.%I::text) > 0)', c.table_name, c.column_name) INTO present;
        IF present THEN
            RAISE EXCEPTION 'Referencia no relacional en %.%; revisar antes de reparar.', c.table_name, c.column_name;
        END IF;
    END LOOP;
END $$;

CREATE TEMP TABLE seed_row_snapshots (table_name text PRIMARY KEY, digest text) ON COMMIT DROP;
DO $$
DECLARE t record; checksum text;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename LOOP
        EXECUTE format('SELECT md5(coalesce(string_agg(row_data, E''\n'' ORDER BY row_data), '''')) FROM (SELECT pg_temp.seed_key(row_to_json(r)::text) AS row_data FROM public.%I r) rows', t.tablename) INTO checksum;
        INSERT INTO seed_row_snapshots VALUES (t.tablename, checksum);
    END LOOP;
END $$;

-- Rekeying is not a domain transition: suppress row lifecycle triggers only in
-- this transaction, then independently verify every FK and every original row.
SET LOCAL session_replication_role = replica;
DO $$
DECLARE c record;
BEGIN
    FOR c IN SELECT table_name, column_name FROM information_schema.columns WHERE table_schema = 'public' AND udt_name = 'uuid' LOOP
        EXECUTE format('UPDATE public.%I t SET %I = m.id FROM seed_id_targets m WHERE t.%I = m.legacy_id', c.table_name, c.column_name, c.column_name);
    END LOOP;
END $$;
SET LOCAL session_replication_role = origin;

DO $$
DECLARE f record; checksum text; broken boolean; join_clause text; nonnull_clause text;
BEGIN
    FOR f IN SELECT s.table_name, s.digest FROM seed_row_snapshots s LOOP
        EXECUTE format('SELECT md5(coalesce(string_agg(row_data, E''\n'' ORDER BY row_data), '''')) FROM (SELECT pg_temp.seed_key(row_to_json(r)::text) AS row_data FROM public.%I r) rows', f.table_name) INTO checksum;
        IF checksum IS DISTINCT FROM f.digest THEN
            RAISE EXCEPTION 'La reparación alteró datos o cantidades en %.', f.table_name;
        END IF;
    END LOOP;
    FOR f IN SELECT c.* FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace WHERE n.nspname = 'public' AND c.contype = 'f' LOOP
        SELECT string_agg(format('p.%I = c.%I', pa.attname, ca.attname), ' AND '),
               string_agg(format('c.%I IS NOT NULL', ca.attname), ' AND ')
        INTO join_clause, nonnull_clause
        FROM unnest(f.conkey, f.confkey) AS k(child, parent)
        JOIN pg_attribute ca ON ca.attrelid = f.conrelid AND ca.attnum = k.child
        JOIN pg_attribute pa ON pa.attrelid = f.confrelid AND pa.attnum = k.parent;
        IF f.confmatchtype <> 's' THEN
            RAISE EXCEPTION 'Tipo de FK no soportado para %; revisar manualmente.', f.conname;
        END IF;
        EXECUTE format('SELECT EXISTS (SELECT 1 FROM %s c WHERE %s AND NOT EXISTS (SELECT 1 FROM %s p WHERE %s))', f.conrelid::regclass, nonnull_clause, f.confrelid::regclass, join_clause) INTO broken;
        IF broken THEN RAISE EXCEPTION 'Referencia inválida tras la reparación: %.', f.conname; END IF;
    END LOOP;
END $$;
SELECT table_name, count(*) AS repaired_ids FROM seed_id_targets GROUP BY table_name ORDER BY table_name;
COMMIT;
\echo 'UUID del seed reparados; filas y claves foráneas verificadas. Volver a iniciar sesión con las cuentas demo.'

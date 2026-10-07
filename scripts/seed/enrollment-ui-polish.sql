-- Optional, isolated UI fixture. Requires the boero-2025 demo catalog first.
-- Run through enrollment-ui-polish.sh; never Flyway/startup. Existing rows are preserved.
\ir enrollment-demo-context.sql
\ir enrollment-demo-ids.sql

DO $$
DECLARE
    tenant uuid := (SELECT institution_id FROM seed_context);
    cycle_year integer := (SELECT academic_year FROM seed_context);
    cycle uuid;
    source_plan uuid;
    path_key uuid := pg_temp.seed_id('ui-polish:', 'path');
    period_key uuid := pg_temp.seed_id('ui-polish:', 'period:' || cycle_year);
    plan_key uuid;
    offering_key uuid;
    level_key uuid;
    space_key uuid;
    course_key uuid;
    class_key uuid;
    day_key uuid;
    schedule_key uuid;
    edition text;
    level_number integer;
    source record;
    teacher record;
    document_key uuid;
    document_number integer;
    ordinal integer := 0;
    starts time;
BEGIN
    SELECT academic_year_id INTO STRICT cycle FROM academic_years
    WHERE institution_id = tenant AND year = cycle_year AND deleted_at IS NULL AND status <> 'CLOSED';
    SELECT p.study_plan_id INTO STRICT source_plan FROM study_plans p
    JOIN training_paths t USING (training_path_id)
    WHERE p.institution_id = tenant AND t.name = 'Ciclo Artístico Vocacional Básico en Instrumento (CAVB)'
      AND p.status = 'ACTIVE' AND p.deleted_at IS NULL;
    IF NOT EXISTS (SELECT 1 FROM courses WHERE study_plan_id = source_plan AND academic_year_id = cycle AND deleted_at IS NULL) THEN
        RAISE EXCEPTION 'Cargar primero el dataset boero-2025 para este ciclo e institución.';
    END IF;

    INSERT INTO training_paths (training_path_id, institution_id, name, description, active, created_at, updated_at)
    VALUES (path_key, tenant, 'Prueba UI · Formación instrumental', 'Datos ficticios para verificar la inscripción. Planes A y B, dos niveles e instrumentos.', true, now(), now())
    ON CONFLICT (training_path_id) DO NOTHING;
    INSERT INTO enrollment_periods (enrollment_period_id, institution_id, academic_year_id, name, start_date, end_date, status, scope_configured, created_at, updated_at)
    VALUES (period_key, tenant, cycle, 'Prueba UI · Inscripción ' || cycle_year,
        make_date(cycle_year, 1, 1)::timestamp AT TIME ZONE (SELECT timezone FROM seed_context),
        make_date(cycle_year + 1, 1, 1)::timestamp AT TIME ZONE (SELECT timezone FROM seed_context) - interval '1 second', 'OPEN', true, now(), now())
    ON CONFLICT (enrollment_period_id) DO NOTHING;

    FOREACH edition IN ARRAY ARRAY['A','B'] LOOP
        plan_key := pg_temp.seed_id('ui-polish:', 'plan:' || edition);
        offering_key := pg_temp.seed_id('ui-polish:', 'offering:' || edition || ':' || cycle_year);
        INSERT INTO study_plans (study_plan_id, institution_id, training_path_id, name, effective_from, status, version_number, created_at, updated_at)
        VALUES (plan_key, tenant, path_key,
            CASE edition WHEN 'A' THEN 'Plan A · Formación instrumental básica'
            ELSE 'Plan B · Formación instrumental y práctica de conjunto' END,
            make_date(cycle_year, 1, 1), 'ACTIVE', 1, now(), now()) ON CONFLICT (study_plan_id) DO NOTHING;
        INSERT INTO enrollment_period_offerings (offering_id, enrollment_period_id, study_plan_id)
        VALUES (offering_key, period_key, plan_key) ON CONFLICT (offering_id) DO NOTHING;
        FOR level_number IN 1..2 LOOP
            level_key := pg_temp.seed_id('ui-polish:', 'level:' || edition || ':' || level_number);
            INSERT INTO academic_levels (academic_level_id, study_plan_id, name, display_order, created_at, updated_at)
            VALUES (level_key, plan_key, 'Nivel ' || level_number, level_number, now(), now()) ON CONFLICT (academic_level_id) DO NOTHING;
            INSERT INTO enrollment_period_offering_levels (offering_level_id, offering_id, academic_level_id)
            VALUES (pg_temp.seed_id('ui-polish:', 'offered:' || level_key || ':' || cycle_year), offering_key, level_key)
            ON CONFLICT (offering_level_id) DO NOTHING;

            FOR source IN SELECT c.*, s.requirement_type, s.approval_mode, s.display_order, a.format
                FROM courses c JOIN study_plan_spaces s USING (study_plan_space_id) JOIN academic_spaces a ON a.academic_space_id = c.academic_space_id
                WHERE c.study_plan_id = source_plan AND c.academic_year_id = cycle AND c.deleted_at IS NULL AND c.status = 'ACTIVE'
                ORDER BY s.display_order, c.instrument_id NULLS FIRST
            LOOP
                space_key := pg_temp.seed_id('ui-polish:', 'space:' || level_key || ':' || source.study_plan_space_id);
                course_key := pg_temp.seed_id('ui-polish:', 'course:' || space_key || ':' || coalesce(source.instrument_id::text, 'none') || ':' || cycle_year);
                class_key := pg_temp.seed_id('ui-polish:', 'class:' || course_key);
                day_key := pg_temp.seed_id('ui-polish:', 'day:' || course_key);
                schedule_key := pg_temp.seed_id('ui-polish:', 'schedule:' || course_key);
                INSERT INTO study_plan_spaces (study_plan_space_id, institution_id, study_plan_id, academic_space_id, academic_level_id, requirement_type, display_order, approval_mode, created_at, updated_at)
                VALUES (space_key, tenant, plan_key, source.academic_space_id, level_key, source.requirement_type, source.display_order, source.approval_mode, now(), now())
                ON CONFLICT (study_plan_space_id) DO NOTHING;
                IF source.instrument_id IS NOT NULL THEN
                    INSERT INTO study_plan_space_instruments (study_plan_space_instrument_id, institution_id, study_plan_space_id, instrument_id, created_at, updated_at)
                    VALUES (pg_temp.seed_id('ui-polish:', 'allowed:' || space_key || ':' || source.instrument_id), tenant, space_key, source.instrument_id, now(), now())
                    ON CONFLICT (study_plan_space_instrument_id) DO NOTHING;
                END IF;
                INSERT INTO courses (course_id, institution_id, study_plan_id, academic_space_id, academic_year_id, status, study_plan_space_id, training_path_id, academic_level_id, instrument_id, created_at, updated_at)
                VALUES (course_key, tenant, plan_key, source.academic_space_id, cycle, 'ACTIVE', space_key, path_key, level_key, source.instrument_id, now(), now())
                ON CONFLICT (course_id) DO NOTHING;
                INSERT INTO course_classes (course_class_id, institution_id, course_id, class_number, created_at, updated_at)
                VALUES (class_key, tenant, course_key, 1, now(), now()) ON CONFLICT (course_class_id) DO NOTHING;
                FOR teacher IN SELECT DISTINCT t.person_id FROM course_class_teachers t JOIN course_classes c USING (course_class_id) WHERE c.course_id = source.course_id LOOP
                    INSERT INTO course_class_teachers (course_class_teacher_id, institution_id, course_class_id, person_id, created_at, updated_at)
                    VALUES (pg_temp.seed_id('ui-polish:', 'teacher:' || class_key || ':' || teacher.person_id), tenant, class_key, teacher.person_id, now(), now())
                    ON CONFLICT (course_class_teacher_id) DO NOTHING;
                END LOOP;
                starts := TIME '14:00' + make_interval(hours => ordinal % 7);
                INSERT INTO course_class_days (course_class_day_id, institution_id, course_class_id, day_of_week, capacity, period_duration_minutes, created_at, updated_at)
                VALUES (day_key, tenant, class_key, (ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'])[1 + ordinal / 7],
                    CASE WHEN source.format = 'GRUPAL' THEN 20 ELSE NULL END, CASE WHEN source.format = 'INDIVIDUAL' THEN 30 ELSE NULL END, now(), now())
                ON CONFLICT (course_class_day_id) DO NOTHING;
                -- Deliberately unavailable variants verify the waitlist warning, without occupying real places.
                IF NOT (edition = 'B' AND level_number = 2 AND source.instrument_id IS NOT NULL AND source.format = 'GRUPAL') THEN
                    INSERT INTO course_class_schedules (course_class_schedule_id, institution_id, course_class_day_id, start_time, end_time, created_at, updated_at)
                    VALUES (schedule_key, tenant, day_key, starts, starts + interval '1 hour', now(), now()) ON CONFLICT (course_class_schedule_id) DO NOTHING;
                    IF source.format = 'INDIVIDUAL' THEN
                        INSERT INTO course_individual_slots (course_individual_slot_id, institution_id, course_class_schedule_id, start_time, end_time, created_at, updated_at)
                        VALUES (pg_temp.seed_id('ui-polish:', 'slot:' || schedule_key), tenant, schedule_key, starts, starts + interval '30 minutes', now(), now())
                        ON CONFLICT (course_individual_slot_id) DO NOTHING;
                    END IF;
                END IF;
                ordinal := ordinal + 1;
            END LOOP;
        END LOOP;
    END LOOP;
    FOR document_number IN 1..2 LOOP
        document_key := pg_temp.seed_id('ui-polish:', 'document:' || document_number);
        INSERT INTO document_definitions (id, institution_id, name, instructions, allowed_formats, active, revision)
        VALUES (document_key, tenant, CASE document_number WHEN 1 THEN 'Prueba UI · Documento de identidad' ELSE 'Prueba UI · Constancia de estudios previos' END,
            'Documento ficticio para probar la interfaz. No cargar documentación personal real.', '["application/pdf", "image/jpeg", "image/png"]', true, 0)
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO training_path_document_requirements (id, training_path_id, institution_id, document_id, level, display_order, active, specific_instructions, revision)
        VALUES (pg_temp.seed_id('ui-polish:', 'requirement:' || document_number), path_key, tenant, document_key,
            CASE document_number WHEN 1 THEN 'AT_SUBMISSION' ELSE 'BEFORE_CONFIRMATION' END, document_number - 1, true,
            'Usá un archivo de prueba, sin datos personales.', 0) ON CONFLICT (id) DO NOTHING;
    END LOOP;
END $$;
COMMIT;
\echo 'Fixture UI cargado: trayecto separado, planes A/B, dos niveles, instrumentos y documentación. Sin sobrescribir datos.'

-- Local enrollment fixture based on the institution website and 2025 registration forms.
-- See docs/enrollment-academic-sources.md for sources and explicit operational assumptions.
-- Atomic, explicit execution only. No Flyway migration and no startup hook.
-- Run with the shared IDs and catalog scripts through make seed-demo.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM people WHERE person_id = md5('boero:enrollment-demo:v1:person:1')::uuid)
       OR EXISTS (SELECT 1 FROM training_paths p JOIN seed_programs s
                  ON p.training_path_id = md5('boero:enrollment:official-2025:path:' || s.code)::uuid)
       OR EXISTS (SELECT 1 FROM study_plans WHERE study_plan_id IN (
                  md5('boero:enrollment-demo:v1:plan:1')::uuid, md5('boero:enrollment-demo:v1:plan:2')::uuid)) THEN
        RAISE EXCEPTION 'Hay IDs del seed anterior. Respaldar y ejecutar make seed-demo-repair-ids antes de cargar.';
    END IF;
END $$;

DO $$
DECLARE
    tenant uuid;
    cycle uuid;
    cycle_year integer := extract(year FROM CURRENT_DATE);
    role_key uuid;
    person_key uuid;
    member record;
    -- Public development-only password: BoeroDemo2026!
    demo_password text := '$2b$12$xwT.Abo0Q4vFReOL.rFEvODBYAegel/G1HE96wmS1S4Rm7VVpAyla';
BEGIN
    -- Stable ID from the existing development seed; the institution slug is editable.
    SELECT institution_id INTO STRICT tenant FROM institutions
    WHERE institution_id = '019e18e4-d919-76d8-9848-7f1b14e64452' AND active;

    FOR member IN SELECT * FROM (VALUES
        (1, '99000001', 'Lucía', 'Ferreyra', DATE '2022-03-10', 'APPLICANT'),
        (2, '99000002', 'Mateo', 'Soria', DATE '1988-11-22', 'APPLICANT'),
        (3, '99000003', 'Camila', 'Roldán', DATE '2003-07-18', 'STUDENT'),
        (4, '99000004', 'Julián', 'Pereyra', DATE '2001-02-09', 'STUDENT'),
        (5, '99000005', 'Valeria', 'Molina', DATE '1986-08-14', 'TEACHER'),
        (6, '99000006', 'Gabriel', 'Acosta', DATE '1979-12-03', 'TEACHER'),
        (7, '99000007', 'Mariana', 'Suárez', DATE '1982-06-27', 'INSTITUTIONAL_AUTHORITY'),
        (8, '99000008', 'Nicolás', 'Herrera', DATE '1990-04-05', 'INSTITUTIONAL_AUTHORITY')
    ) AS members(number, document, first_name, last_name, birth_date, role_code)
    LOOP
        SELECT role_id INTO STRICT role_key FROM roles
        WHERE institution_id = tenant AND code = member.role_code AND is_system;
        person_key := pg_temp.demo_id('person:' || member.number);

        INSERT INTO people (person_id, institution_id, document_number, first_name, last_name,
                            email, birth_date, deleted, created_at, updated_at)
        VALUES (person_key, tenant, member.document, member.first_name, member.last_name,
                lower(translate(member.first_name || '.' || member.last_name, 'áéíóúñÁÉÍÓÚÑ', 'aeiounAEIOUN')) || '@example.test', member.birth_date, false, now(), now())
        ON CONFLICT (person_id) DO UPDATE
        SET birth_date = EXCLUDED.birth_date, updated_at = now();

        INSERT INTO users (user_id, institution_id, person_id, password, enabled,
                           email_verification_status, email_verified_at, created_at, updated_at)
        VALUES (pg_temp.demo_id('user:' || member.number), tenant, person_key, demo_password,
                true, 'VERIFIED', now(), now(), now()) ON CONFLICT (user_id) DO NOTHING;

        INSERT INTO person_role_assignments (person_role_assignment_id, institution_id, person_id,
                                            role_id, access_scope, created_at, updated_at)
        VALUES (pg_temp.demo_id('role:' || member.number), tenant, person_key, role_key,
                'INSTITUTION', now(), now()) ON CONFLICT (person_role_assignment_id) DO NOTHING;

        IF member.role_code = 'STUDENT' THEN
            INSERT INTO students (student_id, institution_id, person_id, enrollment_date,
                                  status, file_number, created_at, updated_at)
            VALUES (pg_temp.demo_id('student:' || member.number), tenant, person_key,
                    CURRENT_DATE, 'ACTIVE', '2026-' || lpad((member.number - 2)::text, 4, '0'), now(), now())
            ON CONFLICT (student_id) DO NOTHING;
        END IF;
    END LOOP;

    SELECT academic_year_id INTO cycle FROM academic_years
    WHERE institution_id = tenant AND year = cycle_year AND deleted_at IS NULL;
    IF cycle IS NULL THEN
        cycle := pg_temp.demo_id('year:' || cycle_year);
        INSERT INTO academic_years (academic_year_id, institution_id, year, start_date, end_date,
                                    status, created_at, updated_at)
        VALUES (cycle, tenant, cycle_year, make_date(cycle_year, 1, 1), make_date(cycle_year, 12, 31),
                CASE WHEN EXISTS (SELECT 1 FROM academic_years WHERE institution_id = tenant AND status = 'ACTIVE')
                     THEN 'PLANNED' ELSE 'ACTIVE' END, now(), now());
    END IF;
    IF EXISTS (SELECT 1 FROM academic_years WHERE academic_year_id = cycle AND status = 'CLOSED') THEN
        RAISE EXCEPTION 'El ciclo actual está cerrado; preparar un ciclo vigente antes de cargar los datos.';
    END IF;

    INSERT INTO academic_years (academic_year_id, institution_id, year, start_date, end_date, status, created_at, updated_at)
    SELECT pg_temp.demo_id('year:' || (cycle_year + 1)), tenant, cycle_year + 1,
           make_date(cycle_year + 1, 1, 1), make_date(cycle_year + 1, 12, 31), 'PLANNED', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM academic_years WHERE institution_id = tenant AND year = cycle_year + 1);

END $$;

-- Replace only the two earlier fixtures created in this task. Never remove user identities,
-- the pre-existing CAV/2008 plan, or enrollment activity. Unexpected references abort everything.
DO $$
DECLARE
    tenant uuid := '019e18e4-d919-76d8-9848-7f1b14e64452';
    old_plans uuid[] := ARRAY[pg_temp.demo_id('plan:1'), pg_temp.demo_id('plan:2')];
    old_paths uuid[] := ARRAY[pg_temp.demo_id('path:1'), pg_temp.demo_id('path:2')];
BEGIN
    PERFORM 1 FROM institutions WHERE institution_id = tenant FOR UPDATE;
    PERFORM 1 FROM institution_enrollment_locks WHERE institution_id = tenant FOR UPDATE;
    PERFORM 1 FROM study_plans WHERE institution_id = tenant AND study_plan_id = ANY(old_plans) FOR UPDATE;
    IF EXISTS (SELECT 1 FROM enrollment_applications
               WHERE institution_id = tenant AND (study_plan_id = ANY(old_plans) OR training_path_id = ANY(old_paths)))
       OR EXISTS (SELECT 1 FROM course_enrollments e JOIN courses c USING(course_id)
                  WHERE c.institution_id = tenant AND c.training_path_id = ANY(old_paths)) THEN
        RAISE EXCEPTION 'La oferta inicial tiene actividad de inscripción; conservarla y revisar su transición antes de reemplazarla.';
    END IF;
    IF EXISTS (SELECT 1 FROM study_plans WHERE institution_id = tenant AND study_plan_id = ANY(old_plans)
               AND (status <> 'ACTIVE' OR name NOT IN ('Plan de Formación Musical Inicial', 'Plan de Formación Instrumental')))
       OR EXISTS (SELECT 1 FROM courses c JOIN study_plan_spaces s USING(study_plan_space_id)
                  WHERE s.study_plan_id = ANY(old_plans) AND c.status <> 'ACTIVE') THEN
        RAISE EXCEPTION 'La oferta inicial cambió de estado; revisar antes de reemplazarla.';
    END IF;

    DELETE FROM enrollment_period_offering_levels l USING enrollment_period_offerings o
    WHERE l.offering_id = o.offering_id AND o.study_plan_id = ANY(old_plans);
    DELETE FROM enrollment_period_offerings WHERE study_plan_id = ANY(old_plans);
    DELETE FROM enrollment_periods p USING academic_years y
    WHERE p.academic_year_id = y.academic_year_id AND p.institution_id = tenant
      AND p.enrollment_period_id IN (pg_temp.demo_id('period:1:' || y.year), pg_temp.demo_id('period:2:' || y.year));

    DELETE FROM course_individual_slots slot USING course_class_schedules schedule,
        course_class_days day, course_classes class, courses course
    WHERE slot.course_class_schedule_id = schedule.course_class_schedule_id
      AND schedule.course_class_day_id = day.course_class_day_id AND day.course_class_id = class.course_class_id
      AND class.course_id = course.course_id AND course.institution_id = tenant AND course.training_path_id = ANY(old_paths);
    DELETE FROM course_class_schedules schedule USING course_class_days day, course_classes class, courses course
    WHERE schedule.course_class_day_id = day.course_class_day_id AND day.course_class_id = class.course_class_id
      AND class.course_id = course.course_id AND course.institution_id = tenant AND course.training_path_id = ANY(old_paths);
    DELETE FROM course_class_days day USING course_classes class, courses course
    WHERE day.course_class_id = class.course_class_id AND class.course_id = course.course_id
      AND course.institution_id = tenant AND course.training_path_id = ANY(old_paths);
    DELETE FROM course_class_teachers teacher USING course_classes class, courses course
    WHERE teacher.course_class_id = class.course_class_id AND class.course_id = course.course_id
      AND course.institution_id = tenant AND course.training_path_id = ANY(old_paths);
    DELETE FROM course_classes class USING courses course
    WHERE class.course_id = course.course_id AND course.institution_id = tenant AND course.training_path_id = ANY(old_paths);
    UPDATE courses SET status = 'INACTIVE' WHERE institution_id = tenant AND training_path_id = ANY(old_paths);
    DELETE FROM courses WHERE institution_id = tenant AND training_path_id = ANY(old_paths);
    DELETE FROM prerequisites WHERE study_plan_id = ANY(old_plans);
    DELETE FROM study_plan_space_instruments i USING study_plan_spaces s
    WHERE i.study_plan_space_id = s.study_plan_space_id AND s.study_plan_id = ANY(old_plans);
    DELETE FROM study_plan_spaces WHERE institution_id = tenant AND study_plan_id = ANY(old_plans);
    DELETE FROM academic_levels WHERE study_plan_id = ANY(old_plans);
    UPDATE study_plans SET status = 'INACTIVE' WHERE institution_id = tenant AND study_plan_id = ANY(old_plans);
    DELETE FROM study_plans WHERE institution_id = tenant AND study_plan_id = ANY(old_plans);
    UPDATE training_paths SET active = false WHERE institution_id = tenant AND training_path_id = ANY(old_paths);
    DELETE FROM training_paths WHERE institution_id = tenant AND training_path_id = ANY(old_paths);
    DELETE FROM academic_spaces WHERE institution_id = tenant AND academic_space_id IN (
        SELECT pg_temp.demo_id('space:' || p || ':' || l || ':' || s)
        FROM generate_series(1, 2) p CROSS JOIN generate_series(1, 2) l CROSS JOIN generate_series(1, 2) s
    );
END $$;

DO $$
DECLARE
    tenant uuid := '019e18e4-d919-76d8-9848-7f1b14e64452';
    cycle uuid;
    cycle_year integer := extract(year FROM CURRENT_DATE);
    program record;
    placement record;
    chosen_instrument record;
    catalog_name text;
    path_key uuid;
    plan_key uuid;
    level_key uuid;
    space_key uuid;
    curriculum_key uuid;
    period_key uuid;
    offering_key uuid;
    course_key uuid;
    class_key uuid;
    day_key uuid;
    schedule_key uuid;
    level_number integer;
    course_number integer := 0;
    period_minutes integer;
    slot_number integer;
    starts time;
    ends time;
    weekday text;
BEGIN
    SELECT academic_year_id INTO STRICT cycle FROM academic_years
    WHERE institution_id = tenant AND year = cycle_year AND deleted_at IS NULL;

    -- Full published instrument catalog; teaching sample below uses piano and guitar.
    FOREACH catalog_name IN ARRAY ARRAY['Guitarra', 'Piano', 'Violín', 'Violoncello', 'Saxofón',
                                       'Clarinete', 'Flauta traversa', 'Trompeta', 'Percusión'] LOOP
        INSERT INTO instruments (instrument_id, institution_id, name, active, created_at, updated_at)
        SELECT pg_temp.academic_id('instrument:' || catalog_name), tenant, catalog_name, true, now(), now()
        WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE institution_id = tenant AND deleted_at IS NULL
                          AND lower(name) = lower(catalog_name));
    END LOOP;
    FOREACH catalog_name IN ARRAY ARRAY['Mañana', 'Tarde', 'Vespertino'] LOOP
        INSERT INTO shifts (shift_id, institution_id, name, active, created_at, updated_at)
        SELECT pg_temp.academic_id('shift:' || catalog_name), tenant, catalog_name, true, now(), now()
        WHERE NOT EXISTS (SELECT 1 FROM shifts WHERE institution_id = tenant AND deleted_at IS NULL
                          AND lower(name) = lower(catalog_name));
    END LOOP;

    FOR program IN SELECT * FROM seed_programs ORDER BY code LOOP
        path_key := pg_temp.academic_id('path:' || program.code);
        plan_key := pg_temp.academic_id('plan:' || program.code);
        INSERT INTO training_paths (training_path_id, institution_id, name, description, active, created_at, updated_at)
        VALUES (path_key, tenant, program.name, NULL, true, now(), now()) ON CONFLICT (training_path_id) DO NOTHING;
        INSERT INTO study_plans (study_plan_id, institution_id, training_path_id, name, effective_from, status, created_at, updated_at)
        VALUES (plan_key, tenant, path_key, program.name || ' - 2025', DATE '2025-01-01', 'ACTIVE', now(), now())
        ON CONFLICT (study_plan_id) DO NOTHING;

        period_key := pg_temp.academic_id('period:' || program.code || ':' || cycle_year);
        offering_key := pg_temp.academic_id('offering:' || program.code || ':' || cycle_year);
        INSERT INTO enrollment_periods (enrollment_period_id, institution_id, academic_year_id, name,
                                        start_date, end_date, status, scope_configured, created_at, updated_at)
        VALUES (period_key, tenant, cycle, 'Inscripciones ' || cycle_year || ' - ' || program.name,
                make_date(cycle_year, 1, 1)::timestamp AT TIME ZONE 'America/Argentina/Cordoba',
                (make_date(cycle_year + 1, 1, 1)::timestamp AT TIME ZONE 'America/Argentina/Cordoba') - interval '1 second',
                'OPEN', true, now(), now()) ON CONFLICT (enrollment_period_id) DO NOTHING;
        INSERT INTO enrollment_period_offerings (offering_id, enrollment_period_id, study_plan_id)
        VALUES (offering_key, period_key, plan_key) ON CONFLICT (offering_id) DO NOTHING;

        FOR level_number IN 1..program.levels LOOP
            level_key := pg_temp.academic_id('level:' || program.code || ':' || level_number);
            INSERT INTO academic_levels (academic_level_id, study_plan_id, name, display_order, description, created_at, updated_at)
            VALUES (level_key, plan_key, 'Nivel ' || level_number, level_number,
                    CASE WHEN program.code = 'cavi' THEN (level_number + 7) || ' años; ' || (level_number + 2) || '° grado escolar'
                         WHEN program.code = 'isfd' THEN level_number || '° año' ELSE NULL END, now(), now())
            ON CONFLICT (academic_level_id) DO NOTHING;
            -- Only level 1 has operational classes. Upper levels remain complete curriculum.
            IF level_number = 1 THEN
                INSERT INTO enrollment_period_offering_levels (offering_level_id, offering_id, academic_level_id)
                VALUES (pg_temp.academic_id('offering-level:' || program.code || ':' || cycle_year), offering_key, level_key)
                ON CONFLICT (offering_level_id) DO NOTHING;
            END IF;
        END LOOP;

        FOR placement IN SELECT * FROM seed_curriculum WHERE seed_curriculum.program = program.code ORDER BY level, position LOOP
            level_key := pg_temp.academic_id('level:' || program.code || ':' || placement.level);
            SELECT academic_space_id INTO space_key FROM academic_spaces
            WHERE institution_id = tenant AND deleted_at IS NULL AND type = placement.type AND format = placement.format
              AND lower(translate(name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) =
                  lower(translate(placement.name, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'));
            IF space_key IS NULL THEN
                space_key := pg_temp.academic_id('space:' || placement.name || ':' || placement.type || ':' || placement.format);
                INSERT INTO academic_spaces (academic_space_id, institution_id, name, type, format, instrumental, active, created_at, updated_at)
                VALUES (space_key, tenant, placement.name, placement.type, placement.format,
                        placement.instrument_group <> 'none', true, now(), now());
            ELSIF EXISTS (SELECT 1 FROM academic_spaces WHERE academic_space_id = space_key
                          AND (instrumental <> (placement.instrument_group <> 'none') OR NOT active)) THEN
                RAISE EXCEPTION 'El espacio existente % tiene otra configuración; no se sobrescribe.', placement.name;
            END IF;
            curriculum_key := pg_temp.academic_id('curriculum:' || program.code || ':' || placement.level || ':' || placement.position);
            INSERT INTO study_plan_spaces (study_plan_space_id, institution_id, study_plan_id, academic_space_id,
                                           academic_level_id, requirement_type, display_order, approval_mode, created_at, updated_at)
            VALUES (curriculum_key, tenant, plan_key, space_key, level_key, 'REQUIRED', placement.position,
                    'PROMOTION_OR_FINAL_EXAM', now(), now()) ON CONFLICT (study_plan_space_id) DO NOTHING;

            IF placement.instrument_group <> 'none' THEN
                INSERT INTO study_plan_space_instruments (study_plan_space_instrument_id, institution_id,
                                                          study_plan_space_id, instrument_id, created_at, updated_at)
                SELECT pg_temp.academic_id('allowed:' || curriculum_key || ':' || i.instrument_id), tenant,
                       curriculum_key, i.instrument_id, now(), now()
                FROM instruments i WHERE i.institution_id = tenant AND i.deleted_at IS NULL AND i.active
                  AND lower(i.name) = ANY(CASE placement.instrument_group WHEN 'harmonic' THEN ARRAY['piano', 'guitarra']
                      ELSE ARRAY['guitarra', 'piano', 'violín', 'violoncello', 'saxofón', 'clarinete', 'flauta traversa', 'trompeta', 'percusión'] END)
                ON CONFLICT (study_plan_space_instrument_id) DO NOTHING;
            END IF;

            IF placement.level <> 1 THEN
                CONTINUE;
            END IF;
            FOR chosen_instrument IN
                SELECT instrument_id, name FROM instruments WHERE institution_id = tenant AND deleted_at IS NULL AND active
                  AND lower(name) IN ('piano', 'guitarra') AND placement.instrument_group <> 'none'
                UNION ALL SELECT NULL::uuid, NULL::varchar WHERE placement.instrument_group = 'none'
                ORDER BY name NULLS FIRST
            LOOP
                course_key := pg_temp.academic_id('course:' || curriculum_key || ':' || coalesce(chosen_instrument.instrument_id::text, 'group') || ':' || cycle_year);
                class_key := pg_temp.academic_id('class:' || course_key);
                day_key := pg_temp.academic_id('day:' || course_key);
                schedule_key := pg_temp.academic_id('schedule:' || course_key);
                -- Sequential afternoon/evening sample: no student or teacher overlaps across the fixture.
                -- This is not the institution's published weekly timetable or required course load.
                weekday := (ARRAY['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'])[course_number / 7 + 1];
                starts := TIME '14:00' + (course_number % 7) * interval '1 hour';
                period_minutes := CASE WHEN placement.format <> 'INDIVIDUAL' THEN NULL
                                       WHEN program.code = 'cavi' THEN 40 ELSE 30 END;
                ends := starts + CASE WHEN period_minutes = 40 THEN interval '40 minutes' ELSE interval '1 hour' END;
                IF weekday IS NULL THEN
                    RAISE EXCEPTION 'La oferta excede los 35 horarios del escenario; definir más comisiones/docentes.';
                END IF;
                course_number := course_number + 1;

                INSERT INTO courses (course_id, institution_id, study_plan_id, academic_space_id, academic_year_id,
                                     status, study_plan_space_id, training_path_id, academic_level_id, instrument_id, created_at, updated_at)
                VALUES (course_key, tenant, plan_key, space_key, cycle, 'ACTIVE', curriculum_key, path_key, level_key,
                        chosen_instrument.instrument_id, now(), now()) ON CONFLICT (course_id) DO NOTHING;
                INSERT INTO course_classes (course_class_id, institution_id, course_id, class_number, created_at, updated_at)
                VALUES (class_key, tenant, course_key, 1, now(), now()) ON CONFLICT (course_class_id) DO NOTHING;
                INSERT INTO course_class_teachers (course_class_teacher_id, institution_id, course_class_id, person_id, created_at, updated_at)
                VALUES (pg_temp.academic_id('teacher:' || course_key), tenant, class_key,
                        pg_temp.demo_id('person:' || CASE WHEN chosen_instrument.name = 'Guitarra' THEN 6
                            WHEN chosen_instrument.name = 'Piano' THEN 5 ELSE 5 + course_number % 2 END), now(), now())
                ON CONFLICT (course_class_teacher_id) DO NOTHING;
                INSERT INTO course_class_days (course_class_day_id, institution_id, course_class_id, day_of_week,
                                               capacity, period_duration_minutes, created_at, updated_at)
                VALUES (day_key, tenant, class_key, weekday, CASE WHEN placement.format = 'GRUPAL' THEN 20 ELSE NULL END,
                        period_minutes, now(), now()) ON CONFLICT (course_class_day_id) DO NOTHING;
                INSERT INTO course_class_schedules (course_class_schedule_id, institution_id, course_class_day_id,
                                                   start_time, end_time, created_at, updated_at)
                VALUES (schedule_key, tenant, day_key, starts, ends, now(), now()) ON CONFLICT (course_class_schedule_id) DO NOTHING;
                IF period_minutes IS NOT NULL THEN
                    FOR slot_number IN 0..(CASE WHEN period_minutes = 40 THEN 0 ELSE 1 END) LOOP
                        INSERT INTO course_individual_slots (course_individual_slot_id, institution_id, course_class_schedule_id,
                                                             start_time, end_time, created_at, updated_at)
                        VALUES (pg_temp.academic_id('slot:' || course_key || ':' || slot_number), tenant, schedule_key,
                                starts + slot_number * make_interval(mins => period_minutes),
                                starts + (slot_number + 1) * make_interval(mins => period_minutes), now(), now())
                        ON CONFLICT (course_individual_slot_id) DO NOTHING;
                    END LOOP;
                END IF;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
COMMIT;
\echo 'Oferta académica cargada desde las planillas 2025. Ver docs/enrollment-academic-sources.md.'

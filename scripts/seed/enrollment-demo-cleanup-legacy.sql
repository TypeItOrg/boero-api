-- Explicit historical cleanup; never part of a normal load. Back up first.
-- Remove only the two fixtures from the original demo dataset. Never remove user identities,
-- the pre-existing CAV/2008 plan, or enrollment activity. Unexpected references abort everything.
DO $$
DECLARE
    tenant uuid := (SELECT institution_id FROM seed_context);
    old_plans uuid[] := ARRAY[pg_temp.demo_id('plan:1'), pg_temp.demo_id('plan:2')];
    old_paths uuid[] := ARRAY[pg_temp.demo_id('path:1'), pg_temp.demo_id('path:2')];
BEGIN
    IF EXISTS (SELECT 1 FROM study_plans WHERE institution_id = tenant AND study_plan_id IN (
        md5('boero:enrollment-demo:v1:plan:1')::uuid, md5('boero:enrollment-demo:v1:plan:2')::uuid)) THEN
        RAISE EXCEPTION 'Las ofertas iniciales todavía usan UUID antiguos; requieren revisión específica antes de limpiarlas.';
    END IF;
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

COMMIT;

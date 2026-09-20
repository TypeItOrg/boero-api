# Cursos, solicitudes y cursadas: reinicio de desarrollo

La migración `20260918184739__add_course_enrollment_core.sql` implementa el modelo nuevo sin intentar inferir el `StudyPlanSpace`, nivel o instrumento de cursos anteriores. Por eso, una base de desarrollo con cursos antiguos debe limpiarse antes de aplicar la migración.

Este procedimiento no está autorizado para staging ni producción.

## Preparación

1. Detener la API para evitar escrituras concurrentes.
2. Confirmar que la conexión apunta a la base local de desarrollo:

```sql
SELECT current_database(), current_user;
```

3. Crear una copia de seguridad antes de modificar datos:

```bash
pg_dump --format=custom --file=boero-dev-before-courses-reset.dump "$DATABASE_URL"
```

4. Verificar que el nombre de la base, host y perfil de ejecución correspondan a desarrollo. No reutilizar el archivo de backup ni la URL de conexión de otro entorno.

## Dependencias que se pueden reiniciar

El bloque omite las tablas que todavía no existen antes de aplicar la migración. En una transacción, y únicamente sobre la base de desarrollo confirmada, eliminar en este orden los datos operativos incompatibles:

```sql
BEGIN;

DO $$
DECLARE table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'course_enrollment_schedules',
        'course_enrollment_histories',
        'enrollment_application_course_assignment_snapshots',
        'course_enrollments',
        'course_individual_slots',
        'enrollment_application_courses',
        'course_waitlist_sequences',
        'course_class_teachers',
        'course_class_schedules',
        'course_class_days',
        'course_classes',
        'courses'
    ] LOOP
        IF to_regclass('public.' || table_name) IS NOT NULL THEN
            EXECUTE format('DELETE FROM public.%I', table_name);
        END IF;
    END LOOP;
END $$;


COMMIT;
```

No eliminar usuarios, personas, instituciones, roles, estudiantes, trayectos, planes, niveles, espacios académicos, instrumentos ni archivos personales de `storage/enrollments`.

Las solicitudes históricas y sus adjuntos no se convierten automáticamente. Pueden conservarse para consulta; las nuevas solicitudes se crean mediante el contexto de trayecto, ciclo y período del modelo nuevo.

## Aplicación y comprobación

Aplicar la migración con el perfil de desarrollo habitual y comprobar:

```bash
./gradlew --no-daemon integrationTest
```

Si la migración falla por una columna `NOT NULL` en `courses`, todavía existen cursos antiguos: detenerse, verificar la base y repetir únicamente el procedimiento de desarrollo después de recuperar el backup si fuera necesario. No modificar una migración ya aplicada ni activar `outOfOrder` en un entorno persistente.

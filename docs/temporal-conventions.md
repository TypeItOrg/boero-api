# Fechas y horas

Los instantes de auditoría, sesiones, bajas y vencimientos se representan con `Instant` en Java y `timestamp(6) with time zone` (`timestamptz`) en PostgreSQL. La API los serializa con offset UTC (`Z`). El frontend puede convertirlos a la zona de presentación, sin agregar o quitar offsets manualmente.

Las fechas de nacimiento, inscripción y vigencia son fechas de calendario: `LocalDate` en Java y `date` en PostgreSQL. Se transmiten como `YYYY-MM-DD` y no se convierten de zona horaria en el frontend.

`TimeConfig` provee un único `Clock` UTC. Los servicios obtienen los instantes mediante `clock.instant()`. `BusinessDateProvider` calcula la fecha actual de Argentina para las reglas de edad, año académico y vigencia de oferta. Las entidades reciben la fecha o el instante como argumento y no consultan el reloj del sistema. La creación de un alumno debe proporcionar explícitamente su fecha de inscripción.

La auditoría JPA y la emisión y validación de JWT usan el reloj compartido. La validación de persistencia usa el validador de Spring. Las conexiones JDBC se inicializan en UTC; la tendencia mensual del dashboard agrupa instantes por mes UTC. Para medir duración de requests se usa `System.nanoTime()`, que no depende de los ajustes de la hora del sistema.

En pruebas, se puede reemplazar el reloj por `Clock.fixed(...)`. Cambiar el reloj no modifica los tipos almacenados ni la zona de negocio.

La migración `20260909174839__normalize_event_timestamps_to_utc.sql` convierte columnas naive existentes a `timestamptz`. `created_at`, `updated_at` y `started_at` se interpretan como UTC porque la auditoría JPA ya escribía `LocalDateTime.now(UTC)`. `expires_at`, `used_at`, `ended_at` y `deleted_at` se interpretan como `America/Argentina/Buenos_Aires` porque se persistían con `LocalDateTime.now()` en la zona de la JVM. La tabla de historial de Flyway y las columnas `date` permanecen fuera de esta conversión. Si un entorno persistió esas columnas con otra zona de JVM, hay que recrearlo en lugar de reaplicar la migración.

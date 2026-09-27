<div align="center">

<br />
<img src="assets/logo.svg" alt="Boero" width="80" height="80" />

# Boero

**Backend seguro y multiinstitucional para la plataforma de gestión académica y administrativa Boero.**

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-FF4438?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)

[![Gradle](https://img.shields.io/badge/Gradle-9.4-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=flat-square&logo=flyway&logoColor=white)](https://documentation.red-gate.com/flyway)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-1.20-2496ED?style=flat-square&logo=docker&logoColor=white)](https://testcontainers.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![GitHub Actions](https://img.shields.io/badge/CI/CD-GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![GHCR](https://img.shields.io/badge/Registry-GHCR-181717?style=flat-square&logo=github&logoColor=white)](https://ghcr.io)

_Centraliza las reglas de negocio, protege el acceso y mantiene aislada la información de cada institución._

</div>

## Almacenamiento de archivos

La guía de preparación del bucket, permisos y activación está en
[Almacenamiento S3](docs/S3.md). La autorización documental, auditoría y limpieza
persistente se describen en [Seguridad documental](docs/DOCUMENT-SECURITY.md). Los requisitos por trayecto,
entregas y admisión provisoria se describen en [Documentación de inscripción](docs/ENROLLMENT-DOCUMENTS.md).

Para descartar adjuntos del esquema anterior de desarrollo antes de migrar: `make discard-legacy-documents` y luego `make dev`. El comando preserva las entregas del modelo nuevo.

Todos los módulos usan `common.storage.StorageService`, con un único proveedor
global. `STORAGE_PROVIDER=local` selecciona `LocalStorageService`; `s3` selecciona
`S3StorageService`, con el mismo contrato de escritura, lectura y eliminación.
Cada módulo aplica sus permisos, límites y validación antes de guardar. La auditoría
y limpieza persistente de inscripciones permanecen en ese módulo.

Para S3, configurar `STORAGE_S3_BUCKET`, `AWS_REGION` y opcionalmente
`STORAGE_S3_PREFIX` (por defecto `boero/`). El SDK usa su cadena estándar de
credenciales; en AWS se recomienda un rol IAM. El bucket debe existir y permitir
`s3:PutObject`, `s3:GetObject` y `s3:DeleteObject` en el prefijo configurado. Al arrancar
se escribe, descarga y elimina un objeto temporal para verificar el acceso.
La configuración inicial usa SSE-S3 y un bucket nuevo sin versionado ni Object Lock.
No se crean buckets ni se modifica su política o ACL.

Usar un bucket privado; no se generan URLs públicas y las descargas siguen pasando
por la autorización de la API. Referencia: [AWS SDK para Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup-project-gradle.html).

`LocalStorageService` guarda los archivos en `STORAGE_LOCAL_DIR` (por defecto,
`storage`). Al iniciar verifica que pueda crear, escribir, leer y eliminar
un archivo temporal; si el almacenamiento no está disponible, la API no arranca.

En desarrollo, Compose fija `/workspace/storage` y monta el volumen
`boero-api-storage-dev`, que sobrevive a recreaciones del contenedor.
En staging y producción, `boero-infra` monta un volumen externo propio de cada
entorno en `/app/storage`; `make prepare` lo crea y la imagen prepara
la carpeta con permisos para `appuser`.

Los backups deben incluir tanto PostgreSQL como este volumen. `make reset-data` elimina los volúmenes locales, incluidos los adjuntos.

Si hay varios períodos de inscripción abiertos para un ciclo lectivo, las solicitudes
nuevas usan el de inicio más reciente; ante un empate se ordenan por UUID descendente.
Las solicitudes existentes conservan su período.

La migración de unicidad de solicitudes activas por postulante y trayecto no elimina
duplicados previos: si existen, su aplicación falla y requieren revisión de negocio.

<div align="center">

<br />
<img src="assets/logo.svg" alt="Boero" width="80" height="80" />

# Boero

**Backend multiinstitucional para la plataforma de gestión académica y administrativa Boero.**

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

## Calidad del código

[Responsabilidades, legibilidad y presupuesto de tamaño](docs/code-quality.md).
Verificación de estructura sin ejecutar tests: `./gradlew codeStructureCheck`.

## Desarrollo local

Preparar la configuración sin sobrescribir un archivo privado existente:

```bash
test -f .env.dev || cp .env.dev.example .env.dev
make dev
```

Los comandos Compose de Make cargan `.env.dev` tanto para interpolar `compose.yaml`
como para definir el entorno del servicio. Si el archivo no existe, se conservan
los defaults de Compose y del perfil `dev`.

La plantilla conserva `http://localhost:3000` como acceso general y
`http://cboero.localhost:3000` para una institución con nombre público `cboero`.
La UI debe tener la misma URL canónica y dominio base. El origen general conserva
el selector de institución; no se redirige a un namespace local adicional.

En Chromium, los nombres terminados en `.localhost` permiten WebAuthn en HTTP,
pero `localhost` se considera un dominio de primer nivel y no puede ser el RP común
de sus subdominios. Con `WEBAUTHN_RP_ID=localhost` en `dev`/`test`, la API resuelve el
RP ID institucional desde el origen validado por el BFF: `localhost` para el acceso
general y `cboero.localhost` para el institucional. La política se aplica tanto a
registro como a autenticación y verificación, manteniendo la validación del origen.

Las passkeys son independientes por hostname local: las existentes de `localhost`
siguen perteneciendo a ese acceso; ingresar con contraseña en `cboero.localhost`
y registrar allí una llave propia. No borrar las anteriores ni recrear la base de
datos. En QA/staging/producción se conserva el RP ID configurado del ambiente, común
al dominio general y sus instituciones.

Referencia: [orígenes WebAuthn admitidos por Chromium](https://chromium.googlesource.com/chromium/src/+/refs/heads/main/content/browser/webauth/origins.md).

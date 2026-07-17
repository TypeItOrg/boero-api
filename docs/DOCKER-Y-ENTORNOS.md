# Docker, entornos y despliegue

## Responsabilidades

Este repositorio es dueño del `Dockerfile`, el entorno local y la publicación de la imagen del API. La topología compartida de staging, PostgreSQL, Redis, Nginx, configuración operativa y rollback vive en [`boero-infra`](https://github.com/TypeItOrg/boero-infra).

`compose.yaml` se usa exclusivamente para desarrollo local. No deben agregarse aquí Compose de staging o producción.

## Desarrollo

```bash
cp .env.dev.example .env.dev
make dev
```

El servicio `dev` usa el target de desarrollo, monta el repositorio y reinicia `bootRun` al detectar cambios. PostgreSQL, Redis y el API publican puertos locales para inspección; estos valores no forman parte del contrato de despliegue.

## Imagen

El target `prod` del `Dockerfile`:

- compila con Java 21 y Gradle Wrapper;
- genera y extrae las capas del `bootJar`;
- ejecuta con un JRE y un usuario sin privilegios;
- incluye el healthcheck de readiness utilizado por infraestructura.

Los pushes a `staging` publican una etiqueta inmutable:

```text
ghcr.io/typeitorg/boero-api:sha-<commit>
```

Después de publicarla, CI ejecuta en la VPS:

```bash
make deploy-api ENV=staging VERSION=sha-<commit>
```

El comando pertenece a `boero-infra`, actualiza únicamente el API y revierte automáticamente si falla su healthcheck. PostgreSQL, Redis y UI no se recrean.

## Flyway y evolución del esquema

Flyway es el único mecanismo que crea o modifica el esquema. Hibernate usa:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Las migraciones comunes viven en `src/main/resources/db/migration` y usan timestamps UTC:

```text
yyyyMMddHHmmss__description.sql
```

Para crear una migración:

```bash
make migration add_description_to_users_table
```

Una migración aplicada no se modifica ni se renombra. Las versiones anteriores de la aplicación deben conservar compatibilidad con las migraciones ya aplicadas para que el rollback sea seguro.

## Operación

El bootstrap de la VPS, variables reales, Nginx, logs, despliegue manual y rollback están documentados en el README de `boero-infra`.

## Producción

`.github/workflows/deploy-production.yaml` permite un despliegue manual futuro. Requiere un SHA completo perteneciente a `main` y el GitHub Environment protegido `production`; no se ejecuta por push.

Antes del primer release deben completarse backups restaurables, TLS, monitoreo, revisión de migraciones y el checklist definido en `docs/PRODUCTION.md` de `boero-infra`.

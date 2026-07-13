# boero-api

Backend de TypeIt para gestión institucional. Está construido con Java 21, Spring Boot 4, Spring Security, JPA/Hibernate, Flyway, PostgreSQL y Redis.

## Estado actual

El proyecto está en desarrollo activo. Actualmente cuenta con:

- Autenticación institucional mediante documento, institución y contraseña.
- Autenticación global para cuentas de plataforma.
- Access tokens JWT y refresh tokens con rotación por familia.
- Sesiones revocables y blacklist de access tokens en Redis.
- Roles y permisos dinámicos con aislamiento por institución.
- Administración de instituciones y personas.
- Protecciones de concurrencia para refresh tokens y autoridades institucionales.
- Pruebas unitarias, MVC, JPA, integración y PostgreSQL con Testcontainers.

Que un módulo no aparezca en esta lista no implica necesariamente un defecto: el producto todavía está incorporando funcionalidades.

## Documentación

El punto de entrada para autenticación, autorización y seguridad es [`docs/auth/INDICE.md`](docs/auth/INDICE.md).

| Documento | Uso recomendado |
|---|---|
| [`AGENTS.md`](AGENTS.md) | Convenciones técnicas y reglas para contribuir al proyecto |
| [`docs/auth/INDICE.md`](docs/auth/INDICE.md) | Recorrido completo por autenticación y autorización |
| [`docs/auth/ESTADO-DE-SEGURIDAD.md`](docs/auth/ESTADO-DE-SEGURIDAD.md) | Garantías implementadas, aislamiento, concurrencia, cachés y checklist para nuevas funcionalidades |
| [`docs/AUTH-INSTITUTIONAL-FOUNDATION.md`](docs/AUTH-INSTITUTIONAL-FOUNDATION.md) | Contexto histórico y técnico de la base institucional |
| [`docs/DOCKER-Y-ENTORNOS.md`](docs/DOCKER-Y-ENTORNOS.md) | Entornos Docker y operación local |

Para sincronizar decisiones dentro del equipo, conviene tratar `ESTADO-DE-SEGURIDAD.md` como contrato técnico vivo: toda funcionalidad que cambie una garantía debería actualizar el documento en el mismo cambio.

## Desarrollo local

Requisitos principales:

- Java 21.
- Docker con Compose.
- El wrapper Gradle incluido en el repositorio.

Preparación:

```bash
cp .env.dev.example .env.dev
make dev
```

Comandos frecuentes:

```bash
./gradlew bootRun
./gradlew test
./gradlew spotlessApply
./gradlew spotlessCheck
```

## Base de datos

PostgreSQL es la base objetivo. Flyway aplica las migraciones versionadas y Hibernate usa `spring.jpa.hibernate.ddl-auto=validate` para comprobar que el esquema coincide con las entidades.

Las migraciones comunes viven en `src/main/resources/db/migration` y siguen la convención UTC `yyyyMMddHHmmss__description.sql`, sin prefijo `V`. Los datos de referencia exclusivos de desarrollo viven en `src/main/resources/db/dev`.

Crear una migración vacía con timestamp UTC:

```bash
make migration add_description_to_users_table
```

Todo cambio persistente requiere actualizar el `@Entity` y agregar una migración nueva. Las migraciones que ya fueron aplicadas no se modifican ni se renombran.

Las garantías dependientes de PostgreSQL —por ejemplo locks pesimistas o advisory locks— deben verificarse mediante Testcontainers y no solamente con H2.

# Docker, Entornos y Despliegue

## Arquitectura

El proyecto usa un único `Dockerfile` y tres stacks de Compose:

- `compose.yaml`: desarrollo con código montado y reinicio automático.
- `compose.staging.yaml`: imagen inmutable, PostgreSQL y Redis persistente.
- `compose.prod.yaml`: misma topología que staging con configuración productiva.

La topología objetivo para un VPS único es:

```text
Internet
  → Nginx del host en 80
  → 127.0.0.1:8080
  → boero-api
      → PostgreSQL en la red de Compose
      → Redis en la red de Compose
```

PostgreSQL y Redis no publican puertos en staging o producción. El puerto del API se enlaza únicamente a loopback, de modo que Nginx sea el único punto de entrada público.

## Imagen de la aplicación

El target `prod` del `Dockerfile`:

- compila con Java 21 y Gradle Wrapper;
- genera y extrae las capas del `bootJar`;
- ejecuta con un JRE, sin Gradle ni código fuente;
- usa el usuario sin privilegios `appuser`;
- arranca mediante `JarLauncher`.

Staging y producción consumen una imagen de GHCR identificada por una etiqueta inmutable:

```text
ghcr.io/typeitorg/boero-api:sha-<commit>
```

GitHub Actions ejecuta `spotlessCheck` y los tests en `develop`, `staging` y `main`. Sólo los pushes a `staging` y `main` publican una imagen. También se publica una etiqueta mutable con el nombre de esas ramas para inspección, pero los despliegues deben usar siempre `sha-<commit>`.

## Desarrollo

Preparación:

```bash
cp .env.dev.example .env.dev
make dev
```

El servicio `dev` usa el target de desarrollo, monta el repositorio en `/workspace` y reinicia `bootRun` al detectar cambios.

PostgreSQL, Redis y el API publican puertos locales para facilitar la inspección durante el desarrollo. Estos defaults no se reutilizan en producción.

## Flyway y evolución del esquema

Flyway es el único mecanismo que crea o modifica el esquema. Hibernate usa:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Un modelo persistente se representa en dos lugares:

- el `@Entity` define el mapping Java/JPA;
- una migración SQL define la transición de la base.

Las migraciones comunes viven en:

```text
src/main/resources/db/migration
```

Los datos exclusivos de desarrollo viven en:

```text
src/main/resources/db/dev
```

Los nombres usan timestamps UTC, sin prefijo `V`:

```text
yyyyMMddHHmmss__description.sql
20260713200130__initial_schema.sql
20260714101532__create_courses.sql
```

Para crear el archivo con el timestamp UTC actual:

```bash
make migration add_description_to_users_table
```

El nombre debe comenzar con una letra minúscula y usar `snake_case`. El comando crea el archivo vacío dentro de `src/main/resources/db/migration` para completar con el cambio SQL correspondiente.

Cada versión debe ser única. Una migración aplicada no se modifica ni se renombra; una corrección se entrega con un timestamp posterior.

El perfil `dev` admite automáticamente como baseline la migración inicial cuando encuentra un volumen histórico creado por Hibernate. Esto permite conservar una base local existente y aplicar desde allí las migraciones posteriores. Staging y producción no habilitan baseline automático: se espera una base nueva o una transición revisada explícitamente.

Los tests rápidos siguen usando H2 con Flyway deshabilitado. La migración completa y los datos de desarrollo se verifican además contra PostgreSQL real mediante Testcontainers y Hibernate `validate`.

## Configuración de staging y producción

Crear el archivo de entorno y restringir sus permisos:

```bash
cp .env.prod.example .env.prod
chmod 600 .env.prod
```

Variables principales:

| Variable | Uso |
|---|---|
| `APP_IMAGE` | Imagen OCI; por defecto `ghcr.io/typeitorg/boero-api` |
| `APP_VERSION` | Etiqueta inmutable `sha-<commit>`; obligatoria |
| `APP_HOST_PORT` | Puerto loopback consumido por Nginx; por defecto `8080` |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Credenciales PostgreSQL obligatorias |
| `JWT_SECRET` | Secreto HS256 obligatorio, aleatorio y de al menos 32 bytes |
| `APP_MEMORY_LIMIT`, `APP_CPU_LIMIT` | Límites del contenedor Java |
| `POSTGRES_MEMORY_LIMIT`, `POSTGRES_CPU_LIMIT` | Límites de PostgreSQL |
| `REDIS_MEMORY_LIMIT`, `REDIS_CPU_LIMIT` | Límites de Redis |

El archivo real no se versiona. En el VPS debe pertenecer al usuario operativo y tener modo `600`.

## Despliegue y rollback

### Primera instalación de staging

El workflow valida los pushes a `develop`, `staging` y `main`, pero sólo publica imágenes desde `staging` y `main`. Para preparar una versión candidata, hacer el merge `develop → staging` y esperar que termine correctamente el job `publish-image` ejecutado sobre `staging`.

Después del merge, obtener el SHA exacto de la rama remota:

```bash
git fetch origin
git rev-parse origin/staging
```

La imagen publicada tendrá la etiqueta inmutable `ghcr.io/typeitorg/boero-api:sha-<sha-completo>`. Aunque CI también publica la etiqueta mutable `staging`, los despliegues deben fijar siempre la etiqueta basada en SHA.

En el VPS, clonar el repositorio para disponer de Compose, el Makefile y los scripts operativos:

```bash
sudo mkdir -p /opt/boero-api
sudo chown "$USER":"$USER" /opt/boero-api
git clone --branch staging git@github.com:TypeItOrg/boero-api.git /opt/boero-api
cd /opt/boero-api
cp .env.staging.example .env.staging
chmod 600 .env.staging
```

Editar `.env.staging` y reemplazar, como mínimo:

```dotenv
APP_VERSION=sha-<sha-completo-del-commit>
DB_NAME=boero_staging
DB_USER=boero
DB_PASSWORD=<secreto-aleatorio>
JWT_SECRET=<secreto-aleatorio-de-al-menos-32-bytes>
PLATFORM_ADMIN_EMAIL=<email-del-administrador-inicial>
PLATFORM_ADMIN_PASSWORD=<contraseña-fuerte-del-administrador-inicial>
```

Estas dos últimas variables crean la primera cuenta administradora de plataforma de forma idempotente. Una vez creada y comprobado el acceso, `PLATFORM_ADMIN_PASSWORD` puede quitarse del archivo y el siguiente arranque omitirá el bootstrap sin borrar la cuenta.

Si el paquete de GHCR es privado, iniciar sesión una sola vez con un personal access token classic que tenga `read:packages`:

```bash
export CR_PAT=<token>
echo "$CR_PAT" | docker login ghcr.io -u <usuario-github> --password-stdin
unset CR_PAT
```

Levantar y verificar el stack:

```bash
make staging
make ps-staging
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

Flyway crea el esquema automáticamente durante el primer arranque. Los datos exclusivos de `dev` no se cargan en staging.

Instalar el proxy en un VPS Debian o Ubuntu:

```bash
sudo cp deploy/nginx/boero-api.conf.example /etc/nginx/sites-available/boero-api
sudo ln -s /etc/nginx/sites-available/boero-api /etc/nginx/sites-enabled/boero-api
sudo unlink /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
curl --fail http://<ip-del-vps>/api/v1/institutions
```

El comando `unlink` sólo es necesario si el sitio default existe. Si ya fue eliminado, se puede omitir.

### Producción

Despliegue de producción:

```bash
make prod
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

`make prod` descarga la imagen indicada en `.env.prod` y recrea el servicio. No compila la aplicación en el VPS.

Para un rollback, cambiar `APP_VERSION` por el SHA anterior y ejecutar nuevamente:

```bash
make prod
```

Flyway sólo avanza el esquema. Una versión anterior de la aplicación debe seguir siendo compatible con las migraciones ya aplicadas; los cambios destructivos deben dividirse en despliegues compatibles.

## Nginx por HTTP

`deploy/nginx/boero-api.conf.example` contiene la configuración actual para acceder por la IP del VPS. Escucha como servidor por defecto en el puerto `80` y no requiere dominio ni certificados.

HTTP no cifra credenciales, tokens ni respuestas. Este staging no debe usar contraseñas o datos reales y conviene restringir el acceso por firewall o VPN si no necesita estar abierto a Internet.

Nginx:

- reenvía tráfico a `127.0.0.1:8080`;
- bloquea `/actuator` desde Internet;
- sobrescribe `X-Forwarded-For` con `$remote_addr`;
- informa host y esquema mediante cabeceras reenviadas.

La aplicación usa el soporte nativo de Tomcat para interpretar esas cabeceras y obtiene la IP desde `HttpServletRequest.getRemoteAddr()`. No debe exponerse el puerto `8080` públicamente ni cambiarse Nginx para conservar un `X-Forwarded-For` enviado por el cliente.

El firewall del VPS debe permitir únicamente los puertos administrativos necesarios y el tráfico público en `80`. HTTPS queda pendiente para cuando staging disponga de dominio.

## Persistencia y límites

PostgreSQL conserva sus datos en `boero-api-postgres-data-<entorno>`.

Redis usa AOF con `appendfsync everysec` y el volumen `boero-api-redis-data-<entorno>`. Esto evita que un reinicio normal elimine inmediatamente la blacklist de JWT.

El API se ejecuta con:

- filesystem raíz de solo lectura;
- `/tmp` temporal;
- capabilities eliminadas;
- `no-new-privileges`;
- límites de CPU y memoria;
- apagado gradual de 30 segundos;
- rotación local de logs Docker.

Los valores por defecto están orientados a un VPS inicial de 2 vCPU y 4 GB de RAM. Deben ajustarse con métricas reales antes de aumentar carga o concurrencia.

## Healthchecks

Readiness incluye:

- estado de readiness de Spring;
- conectividad PostgreSQL;
- conectividad Redis.

Compose consulta:

```text
/actuator/health/readiness
```

El endpoint permanece accesible desde el host por loopback, pero Nginx no lo publica.

Comandos de inspección:

```bash
make ps-prod
docker compose --env-file .env.prod -f compose.prod.yaml logs --tail=200 prod
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

## Alcance operativo actual

La preparación incluye construcción reproducible, registry, migraciones, proxy HTTP mediante Nginx, persistencia de PostgreSQL y Redis, límites, healthchecks y rotación de logs.

La automatización de backups y restauración queda explícitamente fuera del alcance actual.

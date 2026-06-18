# Configuración de Docker y Entornos

Este documento sirve como guía completa del entorno Docker del proyecto, incluyendo desarrollo con recarga automática, staging, producción, PostgreSQL, testing con H2 y operación local paso a paso.

## Introducción

Este proyecto fue preparado para trabajar con una única base técnica y tres entornos operativos claros: `dev`, `staging` y `prod`. En los tres casos la aplicación usa **PostgreSQL**. Los tests, en cambio, no dependen de Docker ni de PostgreSQL porque **Spring levanta H2** en memoria desde `src/test/resources/application.properties`.

La idea principal del setup es mantener una sola forma de construir la aplicación, un solo `Dockerfile`, y separar el comportamiento por entorno usando **Docker Compose** y **perfiles de Spring Boot**. Eso permite que el equipo trabaje con una experiencia consistente y que, al mismo tiempo, cada entorno tenga sus propias reglas de ejecución, nombres de contenedores y persistencia de datos.

## Requisitos previos

Para usar el proyecto localmente hace falta tener instalado **Docker** y **Docker Compose**. Si además querés correr comandos Gradle fuera de contenedor, como `./gradlew test`, también necesitás **Java 21** disponible en la máquina. Una verificación mínima sería ejecutar:

```bash
docker --version
docker compose version
```

## Estructura de la configuración

La base del sistema está distribuida en algunos archivos clave. `Dockerfile` define tanto la imagen de desarrollo como la imagen final de prod. `compose.yaml` contiene el stack de `dev`, `compose.staging.yaml` contiene el stack de `staging` y `compose.prod.yaml` contiene el stack de `prod`.

La configuración de Spring se reparte entre `application.properties`, que contiene lo común, y los archivos por perfil `application-dev.properties`, `application-staging.properties` y `application-prod.properties`. Para los tests se usa `src/test/resources/application.properties`, de modo que H2 quede completamente encapsulado en el contexto de testing.

También hay dos scripts auxiliares en `docker/`. `dev-entrypoint.sh` es el **responsable de arrancar Spring Boot en desarrollo y reiniciarlo cuando detecta cambios en el código**. El script `postgres-entrypoint.sh` extiende el arranque de PostgreSQL para asegurarse de que la base configurada por `DB_NAME` exista incluso si el volumen ya venía de una corrida anterior.

## Archivos Compose por entorno

Cada entorno tiene su propio archivo Compose para evitar que Docker reutilice accidentalmente una imagen de otro entorno:

- `compose.yaml`: `dev`, `postgres` y `redis` para desarrollo local.
- `compose.staging.yaml`: `staging`, `postgres` y `redis` para staging.
- `compose.prod.yaml`: `prod`, `postgres` y `redis` para producción.

Los servicios de aplicación se llaman igual que el entorno (`dev`, `staging`, `prod`) y declaran imágenes distintas (`boero-api:dev`, `boero-api:staging`, `boero-api:prod`). Esto impide que `make prod` use una imagen construida previamente desde el target de desarrollo.

Uso típico (el `Makefile` del repo suele encapsular esto):

```bash
docker compose up --build dev
docker compose --env-file .env.staging -f compose.staging.yaml up --build staging
docker compose --env-file .env.prod -f compose.prod.yaml up --build prod
```

Resumen de cómo se diferencia cada override respecto a la idea de “solo base + entorno”:

| Enfoque | `compose.yaml` | `compose.staging.yaml` | `compose.prod.yaml` |
|--------|--------------------|-------------------------|---------------------|
| **`build.target`** | `dev` | `prod` | `prod` |
| Objetivo | Código montado, caché Gradle, perfil Spring `dev`, entrypoint de desarrollo | Imagen empaquetada, credenciales obligatorias (`${VAR:?…}`), healthcheck HTTP | Igual que staging en tipo de imagen; **`restart: unless-stopped`** en app y Postgres |
| Postgres | Puertos publicados, defaults locales para DB | Credenciales requeridas, healthcheck | Igual + restart |

## Qué son los targets y para qué sirven

En un **Dockerfile multi-stage**, cada bloque que comienza con `FROM imagen AS nombre` define una **etapa**. El **nombre** de esa etapa es lo que Docker y Compose llaman **target** cuando construís la imagen.

**Para qué sirve:** elegís **en qué etapa “cortás” el build**. La imagen final es **solo esa etapa**, no un solo filesystem con todo mezclado. Eso permite:

- **Un solo `Dockerfile`**, varias imágenes posibles (por ejemplo desarrollo vs producción).
- **Imágenes más chicas** en runtime al no incluir JDK, Gradle ni capas de compilación innecesarias.
- En **Docker Compose**, `build.target` indica: “al hacer `docker compose build`, la imagen del servicio debe ser la etapa llamada `X`”.

En una frase: **un target es “construyo hasta esta etapa y esa capa es mi imagen final”**.

### Targets en este proyecto

La cadena para **producción / staging** (imagen liviana con JRE) es:

1. **`deps`** — JDK, resolución de dependencias Gradle (sin `src` todavía).
2. **`builder`** — añade `src` y genera el `bootJar`.
3. **`extract`** — extrae capas del JAR (patrón habitual Spring Boot + Docker).
4. **`prod`** — imagen **`eclipse-temurin:21-jre-jammy`**, usuario no root, `curl` para healthchecks; arranque con `JarLauncher`.

La etapa **`dev`** es **independiente** de esa cadena: parte de **`gradle:9.4.1-jdk21-jammy`**, no hereda de `deps` / `builder` / `extract` / `prod`. Incluye herramientas de desarrollo y el `dev-entrypoint.sh`. Con **`build.target: dev`**, el código se trabaja sobre todo vía **bind mount** (`.: /workspace`); no hace falta que el JAR final esté empaquetado dentro de la imagen para desarrollo día a día.

- Con **`target: prod`**, Docker construye lo necesario para llegar a **`prod`** (típicamente `deps` → `builder` → `extract` → `prod`).
- Con **`target: dev`**, Docker construye **solo la etapa `dev`** (y su imagen base); la rama multi-stage de producción no interviene en esa imagen.

## Cómo funciona cada entorno
Esta sección explica el comportamiento de cada entorno, qué imagen utiliza, cómo arranca la aplicación y qué diferencias operativas existen entre desarrollo, staging y producción.

### Desarrollo

En `dev` **la aplicación corre dentro de Docker** usando el target `dev` del `Dockerfile`. Ese target parte de `gradle:9.4.1-jdk21-jammy`, ya trae Gradle instalado y evita la descarga del wrapper en cada arranque. El contenedor ejecuta `gradle bootRun` y, al mismo tiempo, un watcher basado en `inotifywait` observa cambios en `src`, `build.gradle` y `settings.gradle`. **Cuando detecta una modificación, reinicia la aplicación.**

El código fuente se monta con bind mount dentro de `/workspace`, por eso editar localmente y guardar se refleja dentro del contenedor. Además existe un volumen dedicado para la caché de Gradle, lo que mejora bastante los tiempos después del primer arranque.

### Staging

En `staging` la aplicación ya no corre con `bootRun`, sino con la imagen final del target `prod`. **Ese entorno está pensado para parecerse mucho más a producción.** Sigue usando PostgreSQL, levanta la app empaquetada y expone un healthcheck operativo sobre `http://localhost:8080/actuator/health/readiness`.

### Producción

En `prod` la estrategia es la misma que en `staging`, pero con una configuración más estricta para operación continua. El contenedor de la aplicación usa `restart: unless-stopped`, el contenedor de PostgreSQL también, y ambos tienen nombres explícitos para que la operación sea más clara.

## Dockerfile

El proyecto usa un único `Dockerfile` con cinco targets: `deps`, `builder`, `extract`, `prod` y `dev`.

El target `deps` resuelve dependencias y aprovecha caché de build. El target `builder` compila y genera el `bootJar`. El target `extract` separa las capas del jar de Spring Boot para mejorar el cacheo de la imagen final. El target `prod` construye la imagen liviana para `staging` y `prod` usando `eclipse-temurin:21-jre-jammy`, crea un usuario no root y ejecuta la aplicación con `JarLauncher`. `dev` prepara la imagen orientada a desarrollo, instala `inotify-tools`, precalienta Gradle y deja listo el entorno para `gradle bootRun`.

El resultado es que no hace falta mantener un `Dockerfile` para desarrollo y otro para producción. Toda la lógica vive en un solo lugar y **Compose decide qué target usar según el entorno**.

## Docker Compose

Cada archivo Compose define un stack completo para su entorno. El servicio de aplicación se llama `dev`, `staging` o `prod`; cada stack incluye su propio PostgreSQL, Redis, red y volumen de datos.

En `dev` se activa el target `dev`, se publica `8080` y `5432`, se montan el proyecto y la caché de **Gradle**, se asignan nombres explícitos a contenedores, red y volumen, y se definen defaults locales para la base. En `staging` y `prod` se usa el target `prod`, se activa el perfil **Spring** correcto, se exigen variables de base de datos sin defaults sensibles y se definen healthchecks de aplicación. En `prod` además se habilita la política de restart.

## Nombres explícitos por entorno

En `dev` los contenedores son `boero-api-dev`, `boero-api-postgres-dev` y `boero-api-redis-dev`, la red es `boero-api-network-dev` y los volúmenes son `boero-api-gradle-cache-dev` y `boero-api-postgres-data-dev`.

En `staging` los contenedores son `boero-api-staging`, `boero-api-postgres-staging` y `boero-api-redis-staging`, la red es `boero-api-network-staging` y el volumen de datos es `boero-api-postgres-data-staging`.

En `prod` los contenedores son `boero-api-prod`, `boero-api-postgres-prod` y `boero-api-redis-prod`, la red es `boero-api-network-prod` y el volumen de datos es `boero-api-postgres-data-prod`.

Esto evita compartir accidentalmente red o persistencia entre entornos distintos y hace que la inspección operativa sea mucho más clara.

## Variables de entorno

Los archivos versionables de variables de entorno son `.env.example`, `.env.dev.example`, `.env.staging.example` y `.env.prod.example`. En desarrollo no hace falta crear `.env.dev` porque `make dev` usa defaults definidos en `compose.yaml`. En `staging` y `prod`, en cambio, `DB_NAME`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET` son obligatorias y Compose falla rápido si no están definidas. Los archivos reales de cada entorno pueden crearse localmente cuando se necesite sobrescribir valores y no deben subirse al repositorio.

El archivo de referencia actual es:

```env
SERVER_PORT=8080
DB_HOST=postgres
DB_PORT=5432
DB_NAME=boero_db
DB_USER=boero
DB_PASSWORD=boero
```

Para `staging` y `prod`, o para personalizar desarrollo, se pueden crear archivos locales a partir del ejemplo:

```bash
cp .env.staging.example .env.staging
cp .env.prod.example .env.prod
```

Después cada uno se ajusta según el entorno. En particular, `staging` y `prod` no deben reutilizar credenciales triviales ni valores de ejemplo. No se deben commitear secretos reales en el repositorio.

## PostgreSQL y creación automática de la base

El servicio de **PostgreSQL** no solo levanta la instancia, sino que también garantiza que la base indicada por `DB_NAME` exista. Eso lo hace `docker/postgres-entrypoint.sh`.

El script arranca **PostgreSQL**, espera hasta que responda sobre la base `postgres`, consulta si existe una base con el nombre configurado y, si no existe, ejecuta `CREATE DATABASE`. Esto resuelve muy bien el escenario en el que el volumen ya existe pero el nombre de base cambia entre corridas.

## Healthcheck de la aplicación

El proyecto tiene Actuator habilitado y el healthcheck operativo de `staging` y `prod` usa `GET /actuator/health/readiness`.

La seguridad se ajusta en `SecurityConfig` para permitir el acceso público a `/actuator/health` y `/actuator/health/**` mientras el resto de las rutas quedan autenticadas. Los healthchecks de Compose usan Actuator readiness porque refleja mejor el estado operativo de Spring.

## Testing con H2

Los tests no usan PostgreSQL. Spring toma la configuración desde `src/test/resources/application.properties` y levanta **H2** en memoria. Esto simplifica mucho el flujo local porque `./gradlew test` funciona sin depender de contenedores, puertos o datos persistidos.

## Comandos disponibles

El `Makefile` **centraliza la operación más común del proyecto**. Para el trabajo diario en desarrollo conviene usar `make` o `make dev`. Igual que el frontend, los targets de entorno ejecutan `up --build` para evitar reutilizar una imagen de otro entorno.

Los comandos principales son:

```bash
make dev
make staging
make prod
make build-staging
make build-prod
make down
make logs
make ps
make ps-dev
make ps-staging
make ps-prod
make test
```

`make`, `make dev`, `make logs` y `make ps` apuntan al entorno de desarrollo para mantener comandos cortos en el día a día. `make staging` y `make prod` operan explícitamente sobre sus entornos. `make test` ejecuta `./gradlew --no-daemon test` en la máquina host.

## Primer arranque en una máquina nueva

Para un primer inicio de desarrollo, usá directamente:

```bash
make dev
```

Eso fuerza la construcción de la imagen y deja el sistema listo. A partir de ahí, el comando habitual pasa a ser:

```bash
make dev
```

Una vez levantado, se puede verificar la app con:

```bash
curl http://localhost:8080/actuator/health/readiness
```

Y los tests con:

```bash
make test
```

## Flujo recomendado para desarrollo diario

El flujo normal del equipo debería ser levantar `dev`, editar código localmente, guardar y dejar que el contenedor reinicie Spring automáticamente. Si aparece el mensaje `Setting up watches...`, es normal: corresponde al watcher de archivos. Si aparece `Starting a Gradle Daemon`, también es normal: significa que Gradle está dejando un daemon listo para acelerar las corridas siguientes.

En otras palabras, el camino rápido para el día a día es `make` o `make dev`.

## Troubleshooting

Si el primer `make dev` tarda, lo normal es que Docker esté descargando imágenes base, instalando paquetes del contenedor y resolviendo dependencias del proyecto. **Eso suele pasar una sola vez o cuando la caché fue invalidada.**

Si el puerto `8080` o `5432` ya está ocupado, hay que liberar el puerto o cambiar la publicación correspondiente. Una forma rápida de inspeccionarlo es:

```bash
ss -ltnp | grep 8080
ss -ltnp | grep 5432
```

Si querés resetear la base local de `dev`, primero baja los servicios con `make down` o `make down-dev` y después elimina el volumen `boero-api-postgres-data-dev`. Al volver a levantar, PostgreSQL recreará la base configurada.

Si `./gradlew test` falla por un lock de Gradle, normalmente significa que otro proceso del workspace está usando `.gradle`. En ese caso conviene esperar a que termine o revisar el estado con:

```bash
./gradlew --status
```

## Buenas prácticas

Como criterio general, usar `make` o `make dev` para el trabajo diario, no commitear secretos reales y mantener alineadas las variables `DB_NAME`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET` entre los `.env.*.example`, los `.env` locales opcionales y la configuración documentada. Los defaults de credenciales existen solo para desarrollo; en `staging` y `prod` esas variables deben definirse explícitamente. También conviene no renombrar contenedores, redes ni volúmenes sin una razón clara, porque varios flujos operativos del equipo se apoyan en esos nombres explícitos.

## Estado actual de la configuración

Hoy el proyecto tiene un único `Dockerfile`, entornos separados por **Compose**, **PostgreSQL** como base en `dev`, `staging` y `prod`, tests con **H2** y healthchecks operativos basados en Actuator readiness. La base de datos se crea automáticamente si no existe y el desarrollo corre completamente dentro de **Docker** con reinicio automático al guardar cambios.

## Próximos pasos sugeridos

La base actual ya es operativa, pero a futuro tiene sentido sumar migraciones con **Flyway** o **Liquibase**, semillas de desarrollo, documentación de API y eventualmente automatización CI reutilizando esta misma estructura.

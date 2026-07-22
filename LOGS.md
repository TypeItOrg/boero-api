# Plan de implementación del sistema de logging — Boero API

## 1. Objetivo

Implementar un sistema de logging que permita:

- visualizar logs claros y legibles durante desarrollo;
- mantener logs persistentes en archivos en `staging` y `production`;
- correlacionar todos los eventos pertenecientes a una misma petición HTTP;
- facilitar el diagnóstico de errores en producción;
- evitar ruido innecesario;
- evitar registrar información sensible;
- mantener una solución simple que pueda evolucionar en el futuro hacia logging estructurado o una plataforma centralizada.

La auditoría funcional queda fuera del alcance de esta implementación.

---

## 2. Arquitectura propuesta

```text
HTTP Request
     │
     ▼
RequestLoggingFilter
     │
     ├── genera/recibe requestId
     ├── guarda requestId en MDC
     ├── mide duración
     │
     ▼
Spring Security
     │
     ▼
JwtAuthenticationFilter
     │
     ▼
Controller
     │
     ▼
Use Case / Service
     │
     ▼
Repository
     │
     ▼
HTTP Response
     │
     ▼
RequestLoggingFilter
     │
     ├── registra status y duración
     ├── agrega X-Request-Id
     └── limpia MDC

Todos los logs
     │
     ▼
SLF4J + Logback
     │
     ├── Console
     │     └── docker logs / desarrollo
     │
     └── Rolling File
           ├── boero-api.log
           └── archive/
                ├── boero-api-2026-07-22.0.log.gz
                └── ...
```

No se agregará un nuevo framework de logging.

Se utilizará el stack que ya provee Spring Boot:

```text
SLF4J
  +
Logback
```

---

# 3. Configuración base del logging

## 3.1. Mantener `application.properties`

La configuración se realizará principalmente utilizando:

```text
src/main/resources/
├── application.properties
├── application-dev.properties
├── application-staging.properties
└── application-prod.properties
```

No se agregará inicialmente:

```text
logback-spring.xml
```

Solo se incorporará en el futuro si se necesita una configuración que no pueda resolverse razonablemente mediante las propiedades de Spring Boot.

---

## 3.2. Configuración común

En `application.properties` se definirán las políticas comunes:

```properties
logging.level.root=INFO

logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=30
logging.logback.rollingpolicy.total-size-cap=1GB

logging.pattern.level=%5p [req:%X{requestId:-system}]
```

Esto permitirá obtener logs similares a:

```text
2026-07-22T14:32:18.421 INFO [req:8fa21c...] EnrollmentService : [Enrollment] Approved, enrollmentId: 542
```

El formato general seguirá la convención existente del proyecto:

```text
[Context] Action/message, key1: {}, key2: {}
```

Ejemplo:

```java
log.info(
    "[Institution] Updated successfully, institutionId: {}",
    institutionId
);
```

---

# 4. Configuración por ambiente

## Desarrollo

En `application-dev.properties`:

- logs de la aplicación en `DEBUG`;
- mantener SQL en `DEBUG` mientras siga siendo útil para desarrollo;
- consola legible;
- archivo local opcional/persistente.

Ejemplo conceptual:

```properties
logging.level.ar.edu.utn.frvm.typeit=DEBUG

logging.file.name=logs/boero-api.log
logging.logback.rollingpolicy.file-name-pattern=logs/archive/boero-api-%d{yyyy-MM-dd}.%i.log.gz
```

Agregar al `.gitignore`:

```gitignore
/logs/
```

Así los archivos generados localmente nunca se versionan.

---

## Staging

Mantener:

```properties
logging.level.org.springframework=INFO
logging.level.ar.edu.utn.frvm.typeit=DEBUG
```

El archivo estará dentro del contenedor en:

```text
/app/logs/boero-api.log
```

Con históricos:

```text
/app/logs/archive/
```

Staging permitirá ver `DEBUG` de nuestro código para facilitar diagnóstico antes de producción.

---

## Producción

Mantener:

```properties
logging.level.org.springframework=INFO
logging.level.ar.edu.utn.frvm.typeit=INFO
```

No habilitar `DEBUG` globalmente.

El archivo estará en:

```text
/app/logs/boero-api.log
```

Con históricos comprimidos en:

```text
/app/logs/archive/
```

---

# 5. Implementar correlación mediante `requestId`

Crear:

```text
src/main/java/ar/edu/utn/frvm/typeit/boero_api/
└── common/
    └── logging/
        └── RequestLoggingFilter.java
```

El filtro será responsable de:

1. obtener o generar un `requestId`;
2. guardarlo en MDC;
3. agregarlo a todos los logs generados durante la petición;
4. devolverlo mediante `X-Request-Id`;
5. medir la duración total de la petición;
6. generar el log resumen HTTP;
7. limpiar siempre el MDC al finalizar.

Flujo:

```text
Request
   │
   ├── X-Request-Id válido?
   │       │
   │       ├── sí → reutilizar
   │       └── no → generar
   │
   ▼
MDC.put("requestId", requestId)
   │
   ▼
Aplicación
   │
   ▼
Response
   │
   ├── X-Request-Id: ...
   │
   ▼
MDC.remove("requestId")
```

Es fundamental limpiar el MDC mediante un bloque `finally`.

---

# 6. Validación del `X-Request-Id`

No se debe confiar directamente en cualquier valor recibido desde el cliente.

Si se acepta un:

```http
X-Request-Id
```

entrante, deberá validarse.

Por ejemplo:

- formato UUID;
- longitud limitada;
- sin saltos de línea ni caracteres arbitrarios.

Ante un identificador inválido:

```text
ignorar valor recibido
        ↓
generar nuevo requestId
```

Inicialmente también es válido generar siempre el identificador desde Spring Boot y devolverlo al cliente.

Esto puede evolucionar posteriormente para que Next.js genere o propague el identificador.

---

# 7. Registrar resumen de peticiones HTTP

El `RequestLoggingFilter` registrará:

```text
method
path
status
durationMs
requestId
```

Nunca registrará automáticamente:

```text
Authorization
Cookie
request body
response body
JWT
refresh token
password
query parameters sensibles
```

Ejemplo:

```text
INFO [req:8fa21c] [HTTP] Request completed, method: POST, path: /api/v1/auth/login, status: 401, durationMs: 42
```

## Política de niveles

Para evitar llenar producción de ruido:

### `2xx` y `3xx`

```text
DEBUG
```

Ejemplo:

```text
DEBUG [req:8fa21c] [HTTP] Request completed, method: GET, path: /api/v1/institutions, status: 200, durationMs: 34
```

Estos serán visibles en desarrollo y staging, pero normalmente no en producción.

### `4xx`

```text
INFO
```

Un `400`, `401`, `403` o `404` no representa necesariamente un problema interno del servidor.

No se marcarán automáticamente todos como `WARN`.

### `5xx`

El resumen HTTP podrá registrarse como:

```text
WARN
```

Mientras que la excepción real se registrará una única vez como:

```text
ERROR
```

desde el manejo centralizado de excepciones.

Esto evita tener varios stack traces del mismo error.

---

# 8. Excluir ruido innecesario

No generar logs normales para health checks repetitivos como:

```text
/actuator/health
/actuator/health/readiness
```

Estos requests podrán:

- omitirse del request logging; o
- registrarse únicamente en `DEBUG`.

Esto evita llenar los archivos cada 30 segundos con:

```text
GET /actuator/health/readiness 200
```

---

# 9. Registrar el filtro antes de Spring Security

Actualmente existe:

```text
JwtAuthenticationFilter
```

dentro de la cadena de seguridad.

El nuevo filtro de logging deberá ejecutarse **antes de Spring Security**.

Esto garantiza que también tengan `requestId`:

```text
401 Unauthorized
403 Forbidden
JWT inválido
JWT expirado
errores de autenticación
controllers
use cases
GlobalExceptionHandler
```

Flujo esperado:

```text
RequestLoggingFilter
        ↓
Spring Security
        ↓
JwtAuthenticationFilter
        ↓
Controller
        ↓
Use Case
```

Se recomienda registrarlo explícitamente mediante una configuración dedicada:

```text
config/
└── LoggingConfig.java
```

utilizando un `FilterRegistrationBean`.

Esto deja explícito el orden y evita mezclar la responsabilidad de logging con `SecurityConfig`.

---

# 10. Mantener el `GlobalExceptionHandler`

El proyecto ya tiene un:

```text
GlobalExceptionHandler
```

por lo que no se creará otro mecanismo paralelo.

Actualmente las excepciones inesperadas terminan en:

```java
@ExceptionHandler(Exception.class)
```

Ese seguirá siendo el punto principal para registrar:

```text
ERROR + stack trace
```

Gracias al MDC, automáticamente aparecerá:

```text
ERROR [req:8fa21c] [Exception] Unexpected error ...
```

## Regla

Una excepción inesperada debe generar **un solo log `ERROR` con stack trace**.

Evitar:

```text
Controller          ERROR
Service             ERROR
Repository          ERROR
GlobalException     ERROR
```

para la misma excepción.

Las capas internas deben dejar propagar la excepción cuando no pueden manejarla realmente.

---

# 11. No tratar errores esperados como `ERROR`

Errores como:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
```

son normalmente situaciones esperables del dominio o del protocolo HTTP.

Por ejemplo:

```text
InstitutionNotFoundException
InvalidCredentialsException
AccessDeniedException
MethodArgumentNotValidException
```

no deben generar automáticamente un stack trace `ERROR`.

El `GlobalExceptionHandler` actual deberá conservar este comportamiento.

---

# 12. Incorporar logs progresivamente en los Use Cases

Una vez implementada toda la infraestructura transversal, recorrer los casos de uso existentes.

No agregar logs indiscriminadamente.

La pregunta para decidir si algo merece un `INFO` debe ser:

> ¿Este evento me ayudaría realmente a entender qué pasó en producción?

## Eventos candidatos a `INFO`

Por ejemplo:

```text
[Auth] Login succeeded
[Auth] Session revoked
[Institution] Created successfully
[Institution] Updated successfully
[User] Created successfully
[Role] Created successfully
[Role] Updated successfully
[Role] Assigned successfully
```

Siempre incluyendo identificadores internos cuando aporten contexto:

```java
log.info(
    "[Institution] Updated successfully, institutionId: {}",
    institutionId
);
```

---

# 13. Evitar logs innecesarios

No implementar:

```java
log.info("Entering execute");
log.info("Finding institution");
log.info("Institution found");
log.info("Validating city");
log.info("Saving institution");
log.info("Institution saved");
log.info("Exiting execute");
```

Esto genera ruido y dificulta encontrar eventos importantes.

Preferir:

```java
log.info(
    "[Institution] Updated successfully, institutionId: {}",
    institutionId
);
```

Y utilizar `DEBUG` únicamente cuando exista información técnica realmente útil:

```java
log.debug(
    "[Authorization] Permission snapshot resolved, userId: {}, institutionId: {}",
    userId,
    institutionId
);
```

---

# 14. Política de niveles

## `ERROR`

Fallos inesperados que requieren investigación.

Ejemplos:

```text
Unhandled exception
Database operation failed unexpectedly
Unexpected integration failure
```

Con stack trace.

---

## `WARN`

Situaciones anormales relevantes que el sistema pudo manejar.

Ejemplos:

```text
Redis temporarily unavailable
Unexpected fallback activated
Suspicious authentication condition
Slow operation exceeding threshold
```

No usar `WARN` simplemente porque una operación de negocio fue rechazada correctamente.

---

## `INFO`

Eventos significativos del funcionamiento normal.

Ejemplos:

```text
Institution updated
User created
Role assigned
Session revoked
Login succeeded
```

---

## `DEBUG`

Información útil para diagnóstico técnico.

Ejemplos:

```text
Permission resolution
Cache hit/miss
Filtro aplicado
Cálculos internos relevantes
Detalles de decisiones técnicas
```

No habilitado para el código de la aplicación en producción.

---

# 15. Política de datos sensibles

Nunca registrar:

```text
password
Authorization header
JWT completo
refresh token
cookies de autenticación
JWT secret
encryption keys
credenciales de base de datos
DTOs completos de autenticación
```

Evitar también datos personales cuando no sean necesarios:

```text
documentNumber
email
phoneNumber
address
```

Preferir identificadores internos:

```text
userId
personId
institutionId
roleId
sessionId
```

Ejemplo incorrecto:

```java
log.info("Login request: {}", request);
```

Ejemplo correcto:

```java
log.info(
    "[Auth] Login succeeded, userId: {}, institutionId: {}",
    userId,
    institutionId
);
```

---

# 16. Persistencia en Docker

Este punto requiere cambios también en:

```text
TypeItOrg/boero-infra
```

Los contenedores de staging y producción utilizan filesystem:

```text
read_only: true
```

Por lo tanto, Spring Boot no podrá simplemente crear:

```text
/app/logs/
```

sin proporcionar un volumen escribible.

Se deberá montar almacenamiento persistente específicamente en:

```text
/app/logs
```

Arquitectura:

```text
Container

/app
├── aplicación     ← read-only
└── logs/          ← volumen escribible
    ├── boero-api.log
    └── archive/
```

Se recomienda utilizar un bind mount hacia una ubicación claramente accesible desde el VPS.

Por ejemplo:

```text
/var/log/boero/staging/api/

/var/log/boero/production/api/
```

Montado como:

```text
/app/logs
```

También deberá garantizarse que el usuario del contenedor pueda escribir allí.

El contenedor de producción actualmente ejecuta la aplicación con un usuario no-root, por lo que los permisos del directorio deberán prepararse correctamente durante el aprovisionamiento.

---

# 17. Mantener también los logs de Docker

No eliminar el logging a consola.

La aplicación seguirá escribiendo simultáneamente:

```text
Console
+
File
```

De esta forma se podrá usar:

```bash
docker logs boero-api-staging
```

para diagnóstico rápido.

Y:

```bash
tail -f /var/log/boero/staging/api/boero-api.log
```

para trabajar con el archivo persistente.

El logging driver de Docker seguirá teniendo su propia rotación limitada.

Esto da dos mecanismos complementarios:

```text
docker logs
    ↓
diagnóstico rápido

archivo persistente
    ↓
histórico y búsqueda
```

---

# 18. Pruebas automatizadas

Crear pruebas específicas para:

```text
RequestLoggingFilter
```

## Casos mínimos

### Generación de requestId

Cuando no viene:

```text
request
   ↓
genera requestId
   ↓
X-Request-Id presente en response
```

### Propagación

Si se decide aceptar un `X-Request-Id` válido:

```text
request X-Request-Id
        ↓
MDC
        ↓
response X-Request-Id
```

### RequestId inválido

Debe descartarse y generarse uno nuevo.

### Limpieza de MDC

Después de finalizar:

```java
MDC.get("requestId") == null
```

Debe cumplirse incluso si ocurre una excepción.

### Seguridad

Comprobar que requests que terminan en:

```text
401
403
```

también reciben un:

```http
X-Request-Id
```

### Error inesperado

Un `500` debe:

```text
tener requestId
+
generar ERROR con stack trace
+
no duplicar el mismo ERROR en varias capas
```

---

# 19. Validación manual

## Archivo

Confirmar:

```text
logs/
├── boero-api.log
└── archive/
```

## Rotación

Reducir temporalmente el tamaño máximo durante una prueba y comprobar la generación de:

```text
boero-api.log

archive/
├── boero-api-2026-07-22.0.log.gz
├── boero-api-2026-07-22.1.log.gz
└── ...
```

Luego restaurar:

```text
10MB
```

---

## Persistencia

Recrear el contenedor:

```bash
docker compose down
docker compose up -d
```

Confirmar que los históricos continúan existiendo.

---

## Datos sensibles

Revisar los archivos buscando accidentalmente:

```text
Authorization
Bearer
password
token
JWT
```

y verificar manualmente cualquier coincidencia.

---

# 20. Documentación

Actualizar la sección `Logging` de:

```text
AGENTS.md
```

Mantener la convención existente y agregar:

- significado de cada nivel;
- reglas para `INFO`;
- uso obligatorio de placeholders;
- uso automático de `requestId`;
- política de datos sensibles;
- regla de no duplicar excepciones;
- ejemplos correctos e incorrectos.

---

# 21. Orden de implementación

## LOG-01 — Configuración base

Modificar:

```text
application.properties
application-dev.properties
application-staging.properties
application-prod.properties
.gitignore
```

Configurar:

- niveles;
- archivo;
- rotación;
- compresión;
- retención;
- tamaño máximo;
- patrón con `requestId`.

---

## LOG-02 — Correlación HTTP

Implementar:

```text
RequestLoggingFilter
```

Responsabilidades:

- `requestId`;
- MDC;
- `X-Request-Id`;
- duración;
- method;
- path;
- status;
- limpieza del MDC.

---

## LOG-03 — Orden del filtro

Crear:

```text
LoggingConfig
```

Registrar el filtro antes de Spring Security.

Validar que cubra:

```text
JWT errors
401
403
controllers
use cases
exceptions
```

---

## LOG-04 — Pruebas del filtro

Agregar pruebas automáticas para:

- generación;
- propagación;
- validación;
- limpieza de MDC;
- errores;
- security responses.

---

## LOG-05 — Integración con manejo de excepciones

Revisar:

```text
GlobalExceptionHandler
```

Mantener un único `ERROR` para excepciones inesperadas.

Evitar duplicación de stack traces.

---

## LOG-06 — Logs de negocio relevantes

Recorrer progresivamente:

```text
auth
authorization
institutional
```

Agregar solamente eventos significativos.

No hacer un cambio masivo agregando logs a todos los métodos.

---

## LOG-07 — Revisión de seguridad

Verificar que ningún log exponga:

```text
passwords
JWT
refresh tokens
Authorization
cookies
secrets
datos personales innecesarios
```

---

## LOG-08 — Persistencia en infraestructura

Modificar:

```text
boero-infra
```

para montar almacenamiento escribible en:

```text
/app/logs
```

en:

```text
compose.staging.yaml
compose.production.yaml
```

Preparar correctamente permisos para el usuario no-root del contenedor.

---

## LOG-09 — Validación en staging

Validar:

```text
console
archivo
rotación
requestId
401/403
500
stack traces
persistencia entre deploys
```

Antes de llevarlo a producción.

---

## LOG-10 — Documentación

Actualizar:

```text
AGENTS.md
```

con el estándar definitivo.

---

# 22. Archivos esperados al finalizar

En `boero-api`:

```text
src/main/java/ar/edu/utn/frvm/typeit/boero_api/
├── common/
│   ├── exceptions/
│   │   └── GlobalExceptionHandler.java
│   │
│   └── logging/
│       └── RequestLoggingFilter.java
│
└── config/
    └── LoggingConfig.java

src/main/resources/
├── application.properties
├── application-dev.properties
├── application-staging.properties
└── application-prod.properties

.gitignore
AGENTS.md
```

No se agregará inicialmente:

```text
logback-spring.xml
```

Ni nuevas dependencias de logging en:

```text
build.gradle
```

---

# 23. Criterios de aceptación

La implementación se considera terminada cuando:

- [ ] Los logs siguen apareciendo correctamente en consola.
- [ ] Existe un archivo `boero-api.log`.
- [ ] Los archivos rotan automáticamente.
- [ ] Los históricos se comprimen.
- [ ] Se conservan como máximo 30 días y hasta aproximadamente 1 GB.
- [ ] Cada request posee un `requestId`.
- [ ] El `requestId` aparece automáticamente en los logs asociados.
- [ ] Las respuestas HTTP incluyen `X-Request-Id`.
- [ ] Los errores `401` y `403` también poseen `requestId`.
- [ ] Las excepciones inesperadas generan un único `ERROR` con stack trace.
- [ ] Los errores esperados no generan stack traces innecesarios.
- [ ] Producción no genera logs `DEBUG` de la aplicación.
- [ ] Los health checks no generan ruido constante.
- [ ] No se registran passwords, JWT, tokens ni headers de autorización.
- [ ] Los archivos sobreviven a la recreación o actualización del contenedor.
- [ ] `./gradlew test` finaliza correctamente.
- [ ] `./gradlew spotlessCheck` finaliza correctamente.

---

# Decisión final de diseño

La primera versión debe mantenerse deliberadamente simple:

```text
SLF4J + Logback existente
          │
          ├── Console
          │
          └── Rolling File
                  │
                  ├── rotación
                  ├── compresión
                  └── persistencia

RequestLoggingFilter
          │
          ├── requestId + MDC
          ├── X-Request-Id
          └── resumen HTTP

Use Cases
          │
          └── logs significativos solamente

GlobalExceptionHandler
          │
          └── ERROR centralizado
```

No incorporar inicialmente:

```text
ELK
Elastic
Loki
Grafana
Logstash
logging JSON
tracing distribuido
auditoría funcional
```

La solución queda preparada para agregar estas herramientas en el futuro sin tener que rediseñar la forma en que la aplicación genera logs.

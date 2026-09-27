# Análisis estático

```sh
make static-analysis
# Equivalente; --continue permite ver los hallazgos de todos los analizadores.
./gradlew --continue staticAnalysis
```

El comando analiza `src/main/java` y `src/test/java`, compila sus fuentes y **no
ejecuta tests**, inicia Spring ni necesita PostgreSQL, Redis o Docker.

## Requisitos y cobertura

- JDK 21 para la compilación habitual y Eclipse ECJ.
- JDK 25 para NullAway/Error Prone, con `--release 21`: no cambia la versión de
  Java del artefacto ni del runtime. Gradle debe poder encontrar ambos JDK como
  toolchains. JDK 21.0.2 no ofrece la lectura de anotaciones de tipo del bytecode
  necesaria para este chequeo estricto.
- `nullAwayMain` y `nullAwayTest`: NullAway en modo JSpecify, incluidos genéricos,
  con errores bloqueantes. `RequireExplicitNullMarking` impide agregar clases sin
  un contrato explícito. Todos los paquetes propios de producción y tests usan
  `@NullMarked`; la anotación **no se hereda a subpaquetes**.
- `ecjMain` y `ecjTest`: compilador Eclipse con el perfil de compatibilidad del
  IDE, para miembros/imports no usados, referencias a métodos, tipos sin
  parametrizar, conversiones inseguras, deprecaciones y recursos. Cualquier
  warning falla la tarea. Los contratos JSpecify completos los valida NullAway,
  no el mapeo de anotaciones legacy de ECJ.
- `spotlessCheck` sigue siendo el chequeo de formato; no sustituye estos análisis.

Las versiones están fijadas en `build.gradle` y `gradle/static-analysis.gradle`.
La configuración ECJ está versionada en `config/static-analysis/ecj.properties`;
no depende de `.settings` o `.vscode` locales. Esos archivos locales no se
sobrescriben. El perfil del editor puede producir avisos diferentes si utiliza
otra versión/configuración del compilador.

## Resultados

Los diagnósticos de NullAway aparecen en la consola con archivo y línea. ECJ
además escribe `build/reports/static-analysis/ecj-main.xml` y `ecj-test.xml`.
No hay baseline de warnings ni exclusiones de paquetes propios. CI ejecuta el
mismo comando en un job separado, sin cambiar las suites ni el despliegue.

## Convenciones

- Declarar con `org.jspecify.annotations.Nullable` los filtros, campos y retornos
  que realmente admiten ausencia; mantener no nulos los datos obligatorios.
- En arrays, `byte @Nullable []` marca nullable al array, no a su elemento.
- No sustituir validaciones de dominio por anotaciones: Bean Validation y los
  invariantes de entidades/use cases siguen siendo necesarios.
- `requireNonNull` explicita valores garantizados por una validación previa,
  una consulta de fixture o una API externa. No debe reemplazar errores de negocio
  para datos opcionales o entradas inválidas.
- Lombok genera anotaciones JSpecify y conserva `@Generated`. La única excepción
  de inicialización del builder parcial de `EnrollmentAttachment` está acotada a
  `NullAway.Init`: sus campos se completan antes de construir la entidad.
- En tests, Mockito/Spring inicializan los campos inyectados y JUnit los métodos
  `BeforeEach`/`BeforeAll`. Las excepciones de recursos se limitan a contenedores
  cuya vida útil administra Testcontainers. Los tests también se analizan, no se
  excluyen por contener mocks.
- ECJ ignora únicamente tokens `SuppressWarnings` de otros analizadores; sus
  reglas de nullness, código sin uso y conversiones continúan activas.

Ningún análisis estático demuestra por sí solo el comportamiento de Spring,
JPA, la base de datos o los flujos HTTP; eso requiere ejecutar las suites o
verificaciones de runtime correspondientes.

Referencias: [NullAway/JSpecify](https://github.com/uber/NullAway/wiki/JSpecify-Support),
[ECJ batch compiler](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/tasks/task-using_batch_compiler.htm).

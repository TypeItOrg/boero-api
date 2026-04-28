# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS deps

WORKDIR /workspace

COPY --chmod=0755 gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies

FROM deps AS builder

WORKDIR /workspace

COPY src src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar

FROM builder AS extract

WORKDIR /workspace

RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination build/extracted

FROM eclipse-temurin:21-jre-jammy AS runtime

ARG UID=10001

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && adduser \
        --disabled-password \
        --gecos "" \
        --home "/nonexistent" \
        --shell "/usr/sbin/nologin" \
        --no-create-home \
        --uid "${UID}" \
        appuser

WORKDIR /app
USER appuser

COPY --chown=appuser:appuser --from=extract /workspace/build/extracted/dependencies/ ./
COPY --chown=appuser:appuser --from=extract /workspace/build/extracted/spring-boot-loader/ ./
COPY --chown=appuser:appuser --from=extract /workspace/build/extracted/snapshot-dependencies/ ./
COPY --chown=appuser:appuser --from=extract /workspace/build/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

FROM gradle:9.4.1-jdk21-jammy AS dev

WORKDIR /workspace

USER root

RUN apt-get update \
    && apt-get install --yes --no-install-recommends inotify-tools \
    && rm -rf /var/lib/apt/lists/*

COPY --chmod=0755 gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY docker/dev-entrypoint.sh /usr/local/bin/dev-entrypoint.sh

RUN chmod +x /usr/local/bin/dev-entrypoint.sh

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon help

EXPOSE 8080

CMD ["/usr/local/bin/dev-entrypoint.sh"]

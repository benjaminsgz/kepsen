# syntax=docker/dockerfile:1.7

ARG JDK_IMAGE=eclipse-temurin:21-jdk-jammy

FROM ${JDK_IMAGE} AS base

ARG BUILD_UID=10001
ARG BUILD_GID=10001

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN groupadd --gid "${BUILD_GID}" app \
    && useradd --uid "${BUILD_UID}" --gid app --create-home --shell /bin/bash app

WORKDIR /workspace

ENV GRADLE_USER_HOME=/home/app/.gradle \
    GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.caching=true" \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

RUN mkdir -p \
    /home/app/.gradle \
    kepsen-core \
    kepsen-grpc \
    kepsen-micronaut \
    kepsen-spring-boot-starter \
    && chown -R app:app /workspace /home/app

USER app

FROM base AS deps

COPY --chown=app:app gradlew settings.gradle build.gradle gradle.properties ./
COPY --chown=app:app gradle ./gradle
COPY --chown=app:app kepsen-core/build.gradle ./kepsen-core/build.gradle
COPY --chown=app:app kepsen-grpc/build.gradle ./kepsen-grpc/build.gradle
COPY --chown=app:app kepsen-micronaut/build.gradle ./kepsen-micronaut/build.gradle
COPY --chown=app:app kepsen-spring-boot-starter/build.gradle ./kepsen-spring-boot-starter/build.gradle

RUN sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew

RUN --mount=type=cache,target=/home/app/.gradle,uid=10001,gid=10001 \
    ./gradlew --no-daemon help

FROM deps AS source

COPY --chown=app:app kepsen-core/src ./kepsen-core/src
COPY --chown=app:app kepsen-grpc/src ./kepsen-grpc/src
COPY --chown=app:app kepsen-micronaut/src ./kepsen-micronaut/src
COPY --chown=app:app kepsen-spring-boot-starter/src ./kepsen-spring-boot-starter/src

CMD ["./gradlew", "--no-daemon", "build"]

FROM source AS test

RUN --mount=type=cache,target=/home/app/.gradle,uid=10001,gid=10001 \
    ./gradlew --no-daemon clean test

FROM source AS build

RUN --mount=type=cache,target=/home/app/.gradle,uid=10001,gid=10001 \
    ./gradlew --no-daemon clean build

FROM source AS mtls-smoke

COPY --chown=app:app certs/dev ./certs/dev
COPY --chown=app:app examples/mtls-smoke ./examples/mtls-smoke

RUN --mount=type=cache,target=/home/app/.gradle,uid=10001,gid=10001 \
    ./gradlew --no-daemon -p examples/mtls-smoke cleanTest test --rerun-tasks \
    || (find examples/mtls-smoke -path "*/build/test-results/test/*.xml" -print -exec cat {} \; && false)

FROM scratch AS artifacts

COPY --from=build /workspace/kepsen-core/build/libs/ /kepsen-core/
COPY --from=build /workspace/kepsen-grpc/build/libs/ /kepsen-grpc/
COPY --from=build /workspace/kepsen-micronaut/build/libs/ /kepsen-micronaut/
COPY --from=build /workspace/kepsen-spring-boot-starter/build/libs/ /kepsen-spring-boot-starter/

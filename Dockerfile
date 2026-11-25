# syntax=docker/dockerfile:1

# ---- build stage: JDK + Gradle, not part of the final image ----
FROM eclipse-temurin:25.0.5.1_2-jdk-noble@sha256:c7eef3dfeb81468533ea48c67bddaa8c7ca916454259e4d0d734d12bcff8e125 AS build
WORKDIR /workspace

# Gradle wrapper and build scripts first: the layer with the Gradle distribution and all dependencies
# is rebuilt only when one of these files changes, not on every source code change
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

# tests are not run here (integration tests need Docker) - they run in CI before the image is built
COPY src/main src/main
RUN ./gradlew --no-daemon bootJar \
    && cp build/libs/coupon-service-*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- runtime stage: JRE only ----
FROM eclipse-temurin:25.0.5.1_2-jre-alpine@sha256:69c2a26e9894016e274c880fa542956d005b3755f84db765145185e87405bd89

RUN addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app

WORKDIR /app

# Spring Boot layers ordered from least to most frequently changing, so that a code change
# rebuilds and pushes only the small application layer; files stay owned by root (read-only for the app user)
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER 10001:10001

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "application.jar"]

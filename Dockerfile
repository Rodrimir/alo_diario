# syntax=docker/dockerfile:1.7

# --- Build ----------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-noble AS build
# O Maven Wrapper valida o SHA-256 do .zip da distribuição; sem unzip ele baixaria o .tar.gz.
RUN apt-get update \
 && apt-get install -y --no-install-recommends unzip \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /workspace

# Dependências primeiro, para aproveitar o cache de camadas.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -DskipTests -Dspotless.check.skip=true package \
 && cp target/cuida-backend-*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# --- Runtime --------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S -g 10001 cuida && adduser -S -u 10001 -G cuida cuida
WORKDIR /app

COPY --from=build --chown=cuida:cuida /workspace/extracted/dependencies/ ./
COPY --from=build --chown=cuida:cuida /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=cuida:cuida /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=cuida:cuida /workspace/extracted/application/ ./

USER 10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness >/dev/null || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

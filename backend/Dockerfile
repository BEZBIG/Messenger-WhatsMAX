# Multi-stage build для WhatsMAX backend.
# Stage 1 собирает fat-jar через Gradle; Stage 2 — лёгкий JRE-рантайм.

# ── Stage 1: build ────────────────────────────────────────────────
FROM gradle:8.4-jdk17 AS build
WORKDIR /app

# Сначала только конфиги — Docker layer cache переиспользует, пока они не меняются.
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

# В gradle.properties лежит Windows-путь к JDK для локальной разработки —
# внутри контейнера он не валиден, выкидываем.
RUN sed -i '/org\.gradle\.java\.home/d' gradle.properties \
 && gradle --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN gradle --no-daemon buildFatJar

# ── Stage 2: runtime ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/whatsmax-backend.jar /app/whatsmax-backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/whatsmax-backend.jar"]

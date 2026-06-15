# syntax=docker/dockerfile:1
#
# Multi-stage build for the flight-booking-platform monorepo.
# Build context is the REPO ROOT — the Gradle build needs settings.gradle,
# the wrapper, and BOTH modules (common + booking-service).
#
#   Stage 1 (build):   full JDK, runs the Gradle bootJar task.
#   Stage 2 (runtime): slim JRE, runs the fat jar as a non-root user.

# ---------------------------------------------------------------------------
# Stage 1 — build the Spring Boot fat jar
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle build scaffolding (wrapper + root build files).
COPY gradlew gradlew
COPY gradle/ gradle/
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle

# Both modules: the Spring Boot app depends on the shared library.
COPY common/ common/
COPY booking-service/ booking-service/

# Ensure the wrapper is executable (it may lose the +x bit on some checkouts),
# then build only the booking-service fat jar. --no-daemon: the container is
# single-use, so a long-lived Gradle daemon would just waste memory.
RUN chmod +x gradlew \
    && ./gradlew :booking-service:bootJar --no-daemon

# ---------------------------------------------------------------------------
# Stage 2 — minimal runtime image
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime

# Run as an unprivileged user instead of root.
RUN groupadd --system app && useradd --system --gid app --create-home app

WORKDIR /app

# Copy just the built fat jar from the build stage (glob avoids hardcoding the
# version in the filename, e.g. booking-service-0.0.1-SNAPSHOT.jar).
COPY --from=build /workspace/booking-service/build/libs/*.jar /app/app.jar

# Drop privileges.
USER app

# The Spring Boot web server listens here.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

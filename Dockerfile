# Multi-stage Docker build for Autonomous Performance Tuner Service
FROM gradle:8.6.0-jdk17 AS builder
WORKDIR /app
COPY gradle /app/gradle
COPY build.gradle.kts settings.gradle.kts /app/
COPY agent /app/agent
COPY ml-tuner /app/ml-tuner
COPY target-app /app/target-app

RUN gradle :ml-tuner:installDist --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/ml-tuner/build/install/ml-tuner /app/ml-tuner

ENTRYPOINT ["/app/ml-tuner/bin/ml-tuner"]

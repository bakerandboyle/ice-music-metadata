# ==============================================================
# ICE Music Metadata Service — Multi-stage Dockerfile
#
# Stage 1: Build with Maven + full JDK
# Stage 2: Run on minimal JRE Alpine
#
# Java 25 LTS on Temurin — first LTS with virtual thread
# pinning fix, safe for Hibernate at scale.
# ==============================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /build

# Cache Maven dependencies (layer reuse on code-only changes)
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src/ src/
RUN ./mvnw package -Dmaven.test.skip=true -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:25-jre-alpine

# Security: non-root user
RUN addgroup -S ice && adduser -S ice -G ice

WORKDIR /app

# Copy the fat JAR from build stage
COPY --from=build /build/target/*.jar app.jar

# Production JVM flags: ZGC for sub-1ms p99 pauses
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -Djdk.tracePinnedThreads=short"

# Expose service and actuator port
EXPOSE 8080

# Run as non-root
USER ice

# Virtual threads + ZGC + graceful shutdown
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

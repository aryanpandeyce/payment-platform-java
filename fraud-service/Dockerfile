# Multi-stage hardened Dockerfile for fraud-service
# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Copy POM files for optimal dependency layer caching
COPY pom.xml .
COPY common/pom.xml common/
COPY identity-service/pom.xml identity-service/
COPY account-service/pom.xml account-service/
COPY payment-service/pom.xml payment-service/
COPY fraud-service/pom.xml fraud-service/
COPY notification-service/pom.xml notification-service/
COPY api-gateway/pom.xml api-gateway/

# Copy sources for common and fraud-service
COPY common/src common/src
COPY fraud-service/src fraud-service/src

# Build common library and fraud-service
RUN mvn clean package -pl :common,:fraud-service -am -DskipTests

# Stage 2: Layer extractor using Spring Boot 3.4 tools
FROM eclipse-temurin:21.0.6_7-jre-alpine AS extractor
WORKDIR /extracted
COPY --from=build /workspace/fraud-service/target/fraud-service-0.0.1-SNAPSHOT.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination /extracted

# Stage 3: Minimal secure non-root runtime
FROM eclipse-temurin:21.0.6_7-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup
WORKDIR /app

# Copy cached dependencies first, then application jar
COPY --from=extractor --chown=appuser:appgroup /extracted/lib/ ./lib/
COPY --from=extractor --chown=appuser:appgroup /extracted/*.jar ./app.jar

ENV SERVER_PORT=8087
EXPOSE 8087

# Actuator liveness probe healthcheck
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:${SERVER_PORT}/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Duser.timezone=Asia/Kolkata", \
  "-jar", "app.jar"]

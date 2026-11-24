# ── Backend Dockerfile (multi-stage) ─────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup -S connectly && adduser -S connectly -G connectly
USER connectly

# Copy JAR
COPY --from=build /build/target/connectly-backend-*.jar app.jar

# Upload volume mount point
RUN mkdir -p /app/uploads

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

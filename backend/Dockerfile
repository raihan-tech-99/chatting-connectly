# ── Backend Dockerfile (multi-stage) ─────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user and uploads dir (as root, before USER switch)
RUN addgroup -S connectly \
 && adduser -S connectly -G connectly \
 && mkdir -p /app/uploads \
 && chown -R connectly:connectly /app

# Switch to non-root user
USER connectly

# Copy JAR (chown to connectly so user can read it)
COPY --chown=connectly:connectly --from=build /build/target/connectly-backend-*.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

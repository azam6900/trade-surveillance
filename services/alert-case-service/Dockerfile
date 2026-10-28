# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn -B -q -pl services/alert-case-service -am -DskipTests package

# ---------- Stage 2: run ----------
FROM eclipse-temurin:21-jre AS runner
WORKDIR /app
RUN groupadd -g 10001 appgroup && useradd -u 10001 -g appgroup -s /bin/false appuser
COPY --from=builder --chown=appuser:appgroup /build/services/alert-case-service/target/*.jar app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
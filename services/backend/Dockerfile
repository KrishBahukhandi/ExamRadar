# ==============================================================================
# Build Stage: Compile and Package Spring Boot Application
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile source code
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Runtime Stage: Lightweight, Non-Root Production Container
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Krish Bahukhandi <dev@examradar.in>"
LABEL description="ExamRadar India Backend & Ingestion Engine"

# Create unprivileged system group and user
RUN addgroup -S examradar && adduser -S examradar -G examradar

WORKDIR /app

# Copy executable fat-JAR from build stage
COPY --from=builder /app/target/examradar-backend-*.jar app.jar

# Set ownership
RUN chown -R examradar:examradar /app

# Switch to unprivileged user
USER examradar:examradar

EXPOSE 8085 10000

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]

# =========================
# Stage 1: Build the app
# =========================
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy Maven wrapper and settings
COPY mvnw .
COPY .mvn .mvn

# Copy pom.xml first to cache dependencies
COPY pom.xml .

# Download dependencies only (cached)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build JAR without tests
RUN ./mvnw clean package -DskipTests

# =========================
# Stage 2: Run the app
# =========================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy built jar from builder
COPY --from=builder /app/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]

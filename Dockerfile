# Stage 1: Build application with Maven
FROM maven:3.9.8-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy Maven descriptor and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-privileged system user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy compiled jar from build stage
COPY --from=builder /build/target/triage-service-1.0.0.jar app.jar

# Expose default HTTP port
EXPOSE 8080

# Configure JVM options for cloud environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

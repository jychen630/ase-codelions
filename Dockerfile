# Use OpenJDK 11 for compilation and runtime
FROM maven:3.8.6-openjdk-11 AS builder

# Set working directory
WORKDIR /app

# Copy Maven files first for better caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:11-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create data directory for H2 database
RUN mkdir -p /app/data

# Expose the port
EXPOSE 18060

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=18060

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:18060/mcp/health || exit 1

# Run the application
CMD ["java", "-jar", "app.jar"]

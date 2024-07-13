# Stage 1: Build the application with Gradle
FROM gradle:7.5.1-jdk17 AS builder

# Set the working directory inside the builder stage
WORKDIR /build

# Copy the Gradle wrapper and build scripts
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY eialib/ eialib/

# Copy the rest of the application source code
COPY src/ src/

# Run the Gradle 'fatJar' task to build the application
RUN ./gradlew fatJar

# Stage 2: Create the final image with Amazon Corretto 22
FROM amazoncorretto:22

# Set the working directory inside the final stage
WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /build/build/libs/myapp-all.jar /app/myapp.jar

# Expose the port on which the application runs
EXPOSE 8080

# Define the entry point to run the application
ENTRYPOINT ["java", "-jar", "/app/myapp.jar"]

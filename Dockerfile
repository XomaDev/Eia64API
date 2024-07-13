# Stage 1: Build the application
FROM gradle:8.5.0-jdk17 as builder

# Copy source code
WORKDIR /app

COPY ./gradlew .
COPY ./gradlew.bat .
COPY ./build.gradle.kts .
COPY ./settings.gradle.kts .
COPY ./gradle ./gradle
COPY ./KEY .
# Refresh dependencies
RUN ./gradlew --refresh-dependencies

RUN ls
# Use Gradle to build the project
# Assuming your build.gradle has a task named 'fatJar' that produces an executable fat jar.
COPY . .
RUN gradle fatJar

# Stage 2: Run the application
FROM gradle:8.5.0-jdk17

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar /app/app.jar
COPY --from=builder /app/KEY /app/KEY

# Command to run the application
CMD ["java", "-jar", "/app/app.jar"]

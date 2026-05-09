# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application, skipping tests to speed up the process
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
# We MUST use a JDK image (not JRE) because the judge engine relies on `javac` to compile user submissions
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install any required OS utilities if necessary (e.g., bash)
# RUN apt-get update && apt-get install -y bash && rm -rf /var/lib/apt/lists/*

# Copy the shaded uber-jar from the builder stage
# (Assuming your pom.xml generates a shaded jar. If the jar name differs, update this)
COPY --from=builder /app/target/*.jar /app/codeverdict.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/codeverdict.jar"]

# Use a lightweight base image with Java 17
FROM openjdk:17-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file to the container
COPY WeatherDataService.jar /app/WeatherDataService.jar

# Expose any necessary ports
EXPOSE 6565

# Set the entrypoint command to run your JAR file
ENTRYPOINT ["java", "-jar", "/app/WeatherDataService.jar"]
# Use a lightweight base image with Java 17
FROM openjdk:17-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file to the container
COPY BaseCentralStation.jar /app/BaseCentralStation.jar

# Set the entrypoint command to run your JAR file
ENTRYPOINT ["java", "-jar", "/app/BaseCentralStation.jar"]

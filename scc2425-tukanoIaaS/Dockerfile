# 1st stage: copy de code and dependencies and build the project
FROM maven:3.8.8-eclipse-temurin-17 AS builder_image

WORKDIR /app
COPY . .

RUN mvn clean compile package -DskipTests -X

# 2nd stage: deploy the apk
FROM tomcat:10-jdk17-corretto

# Set the working directory in the container
WORKDIR /usr/local/tomcat

# Copy the WAR file from the Maven build
COPY --from=builder_image /app/target/*.war ./webapps/

# Expose the default Tomcat port
EXPOSE 8080

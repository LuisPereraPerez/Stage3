FROM openjdk:21-jdk-slim

WORKDIR /app
COPY target/Indexer-1.0-SNAPSHOT.jar app.jar
EXPOSE 5704
ENTRYPOINT ["java", "-jar", "app.jar"]

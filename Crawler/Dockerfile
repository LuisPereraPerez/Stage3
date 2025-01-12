# Usa una imagen base con JDK 17
FROM eclipse-temurin:17-jdk

# Establece el directorio de trabajo en /app
WORKDIR /app

# Copia el archivo JAR al contenedor
COPY ./Crawler/target/Crawler-1.0-SNAPSHOT.jar app.jar

# Expone el puerto de Hazelcast
EXPOSE 5701

# Ejecuta la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
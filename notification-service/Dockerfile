FROM eclipse-temurin:21-jre
WORKDIR /app
COPY notification-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
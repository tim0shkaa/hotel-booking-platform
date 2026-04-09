FROM eclipse-temurin:21-jre
WORKDIR /app
COPY payment-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
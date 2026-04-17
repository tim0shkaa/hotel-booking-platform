FROM eclipse-temurin:21-jre
WORKDIR /app
COPY review-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
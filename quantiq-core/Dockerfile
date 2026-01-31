FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy pre-built JAR
COPY build/libs/quantiq-core-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

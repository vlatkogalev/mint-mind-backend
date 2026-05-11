FROM gradle:8.14.0-jdk21 AS build

WORKDIR /workspace

COPY . .

RUN chmod +x gradlew && ./gradlew :app:api:buildFatJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/app/api/build/libs/app-all.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

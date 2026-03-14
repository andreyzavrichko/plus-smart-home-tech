FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn package -pl infra/discovery-server -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/infra/discovery-server/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
FROM maven:3.10.0-eclipse-temurin-25-noble AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jre-noble
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
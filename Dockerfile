FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --create-home --uid 10001 unicircle
COPY --from=build /app/target/unicircle-0.0.1-SNAPSHOT.jar /app/unicircle.jar
USER unicircle
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/unicircle.jar"]

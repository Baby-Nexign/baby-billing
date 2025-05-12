FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .

RUN mvn verify --fail-never

COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-jammy

RUN groupadd -r springgroup && \
    useradd -r -g springgroup -u 1001 springuser

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown -R springuser:springgroup /app

USER springuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

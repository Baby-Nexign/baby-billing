FROM maven:3.8.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

RUN groupadd -r springgroup && \
    useradd -r -g springgroup -u 1001 springuser

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown -R springuser:springgroup /app

USER springuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
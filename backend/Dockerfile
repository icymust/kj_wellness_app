# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# --- runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# эти переменные удобно переопределять из docker-compose
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/ndl
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=postgres
ENV SERVER_PORT=8080

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]

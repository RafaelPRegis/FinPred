# Build stage
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

# Copiar arquivos necessários para build
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Converter quebras de linha e compilar usando cache para dependências do Maven
RUN apt-get update && apt-get install -y dos2unix && dos2unix mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

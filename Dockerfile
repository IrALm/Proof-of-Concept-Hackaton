# --------- Étape 1 : Build Maven ---------
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copier le projet
COPY pom.xml .
COPY src ./src

# Build du jar
RUN mvn clean package -DskipTests

# --------- Étape 2 : Runtime ---------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copier le jar depuis l'étape build
COPY --from=build /app/target/*.jar app.jar

# Port (optionnel mais propre)
EXPOSE 8000

# Lancement
ENTRYPOINT ["java","-jar","app.jar"]
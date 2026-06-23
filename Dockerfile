# ====================================================
# Dockerfile — SmartFleet Backend
# Multi-stage build pour optimiser la taille de l'image
# ====================================================

# Étape 1 : Build de l'application avec Maven et JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copie du fichier POM pour télécharger les dépendances (mise en cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copie des sources
COPY src ./src

# Compilation et empaquetage de l'application (sans exécuter les tests)
RUN mvn package -DskipTests -B

# Étape 2 : Exécution de l'application avec JRE 17 (plus léger)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copie du JAR généré depuis l'étape de build
COPY --from=build /app/target/backend-1.0.0-SNAPSHOT.jar app.jar

# Port exposé par le conteneur (Spring Boot par défaut)
EXPOSE 8080

# Démarrage de l'application
ENTRYPOINT ["java", "-jar", "app.jar"]

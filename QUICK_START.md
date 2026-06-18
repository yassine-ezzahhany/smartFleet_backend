# 🚀 SmartFleet Backend - Quick Start

Démarrez le projet en 5 minutes!

## 1️⃣ Cloner et Configurer

```bash
# Se placer dans le répertoire du projet
cd backend

# Copier l'exemple de configuration
cp src/main/resources/application-dev.properties src/main/resources/application-dev.properties

# (Si vous avez Firebase)
cp src/main/resources/firebase-config-example.json src/main/resources/firebase-config.json
```

## 2️⃣ Démarrer les Services (Docker)

```bash
# Démarrer PostgreSQL, Valhalla, Redis
docker-compose up -d

# Vérifier que les services sont up
docker-compose ps

# Voir les logs
docker-compose logs -f postgres
```

## 3️⃣ Créer la Base de Données

```bash
# Connecter à PostgreSQL
psql -h localhost -U smartfleet_user -d smartfleet_db_dev

# Créer l'extension PostGIS (si elle n'existe pas)
CREATE EXTENSION IF NOT EXISTS postgis;

# Vérifier
SELECT version();  -- Doit montrer PostGIS
SELECT postgis_version();

# Quitter
\q
```

## 4️⃣ Build et Démarrage

```bash
# Build Maven
mvn clean install -DskipTests

# Démarrer en développement
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# OU avec Java directement
java -jar target/backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

L'application démarre sur **http://localhost:8080/api**

## 5️⃣ Tester l'API

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# Réponse attendue:
# {"status":"UP"}
```

## ✅ Prêt!

- **API:** http://localhost:8080/api
- **WebSocket:** ws://localhost:8080/api/ws
- **PostgreSQL:** localhost:5432 (user: smartfleet_user)
- **Valhalla:** http://localhost:8002
- **pgAdmin:** http://localhost:5050 (si profile dev)

## 🛠️ Commandes Utiles

```bash
# Voir les logs en temps réel
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" 2>&1 | tee app.log

# Arrêter les services Docker
docker-compose down

# Nettoyer les volumes Docker
docker-compose down -v

# Rebuild Maven proprement
mvn clean install -U

# Tests
mvn test

# Code coverage
mvn clean test jacoco:report
```

## 🔧 Configuration Rapide

**JWT Security:** Modifier `application.properties` ou variables d'environnement (`.env`)
```properties
app.security.jwt.secret=YOUR_JWT_SECRET
app.security.jwt.expiration-ms=86400000
```

**Valhalla:** S'il est hébergé ailleurs
```properties
valhalla.service.url=http://your-valhalla-host:8002
```

## 📚 Documentation Complète

- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture en détail
- [README.md](README.md) - Documentation complète
- [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) - Guide d'implémentation
- [DELIVERABLES.md](DELIVERABLES.md) - Récapitulatif des livrables

## 🆘 Troubleshooting

### Port 8080 déjà utilisé
```bash
# Trouver le processus
lsof -i :8080

# Tuer le processus (PID)
kill -9 <PID>

# Ou changer le port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Erreur PostgreSQL
```bash
# Vérifier les logs Docker
docker-compose logs postgres

# Réinitialiser la base
docker-compose down -v
docker-compose up -d postgres

# Attendre quelques secondes...
sleep 10
```

### Erreur Valhalla
```bash
# Vérifier les logs
docker-compose logs valhalla

# Peut prendre quelques minutes au premier démarrage
# Vérifier l'endpoint
curl http://localhost:8002/health
```

---

**Prêt à contribuer? Voir [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)**

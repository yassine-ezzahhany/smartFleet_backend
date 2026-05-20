# SmartFleet Backend 🚚📦

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=java)](https://www.java.com)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-PostGIS-336791?style=flat-square&logo=postgresql)](https://www.postgresql.org)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

Backend d'une application de gestion logistique complète avec optimisation des tournées, calcul d'itinéraires en temps réel, et suivi de livraison.

## 🎯 Fonctionnalités

- ✅ **Authentification sécurisée** avec JWT (Clerk)
- ✅ **Optimisation des tournées** avec OR-Tools (Vehicle Routing Problem)
- ✅ **Calcul d'itinéraires** avec Valhalla
- ✅ **Traçabilité en temps réel** via WebSocket STOMP
- ✅ **Notifications push** avec Firebase Cloud Messaging
- ✅ **Gestion multi-rôles** (Admin, Manager, Driver, Client)
- ✅ **Support données spatiales** avec PostGIS

## 🏗️ Architecture

Le projet suit une **architecture hexagonale (clean architecture)** divisée en 6 couches :

1. **Domain** - Logique métier (indépendante des frameworks)
2. **Application** - Orchestration des services (use cases)
3. **Infrastructure** - Détails techniques (BD, services externes)
4. **Presentation** - API REST et WebSocket
5. **Shared** - Utilitaires et constantes

Voir [ARCHITECTURE.md](ARCHITECTURE.md) pour plus de détails.

## 🚀 Démarrage Rapide

### Prérequis

- **Java 17+**
- **Maven 3.8+**
- **PostgreSQL 14+** avec extension PostGIS
- **Docker** (optionnel, pour les services externes)

### Installation

#### 1. Préparation de la Base de Données

```bash
# Créer la base de données
createdb smartfleet_db_dev

# Installer l'extension PostGIS
psql smartfleet_db_dev -c "CREATE EXTENSION postgis;"

# Créer l'utilisateur
createuser smartfleet_user
psql -c "ALTER USER smartfleet_user WITH PASSWORD 'smartfleet_password';"

# Donner les permissions
psql smartfleet_db_dev -c "GRANT ALL PRIVILEGES ON DATABASE smartfleet_db_dev TO smartfleet_user;"
```

#### 2. Configuration de l'Application

Mettre à jour `application-dev.properties` :

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/smartfleet_db_dev
spring.datasource.username=smartfleet_user
spring.datasource.password=smartfleet_password

# Clerk JWT
app.security.clerk.issuer=https://your-clerk-domain
app.security.clerk.audience=https://your-clerk-domain

# Valhalla (service local ou distant)
valhalla.service.url=http://localhost:8002
```

#### 3. Build et Démarrage

```bash
# Compiler le projet
mvn clean install

# Démarrer en mode développement
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Ou directement avec Java
java -jar target/backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

L'application sera disponible sur `http://localhost:8080/api`

### Docker Compose (Services Externes)

Pour démarrer les services externes (PostgreSQL, PostGIS, Valhalla) :

```bash
docker-compose up -d
```

## 📁 Structure du Projet

```
backend/
├── pom.xml                          # Dépendances Maven
├── ARCHITECTURE.md                  # Documentation d'architecture
├── README.md                        # Ce fichier
│
├── src/main/
│   ├── java/ma/smartfleet/backend/
│   │   ├── domain/                 # Couche domaine
│   │   ├── application/            # Couche application
│   │   ├── infrastructure/         # Couche infrastructure
│   │   ├── presentation/           # Couche présentation
│   │   ├── shared/                 # Utilitaires
│   │   └── BackendApplication.java # Classe principale
│   │
│   └── resources/
│       ├── application.properties        # Config par défaut
│       ├── application-dev.properties    # Config développement
│       ├── application-prod.properties   # Config production
│       └── firebase-config-example.json  # Exemple Firebase
│
└── src/test/                        # Tests unitaires et d'intégration
```

## 🔌 Intégrations Externes

### OR-Tools (Optimisation des Tournées)

L'adaptateur `ORToolsAdapter` résout le Vehicle Routing Problem :

```java
OptimizationResult result = orToolsAdapter.optimizeDeliveries(
    orders,      // Commandes à livrer
    vehicles,    // Véhicules disponibles
    drivers      // Conducteurs disponibles
);
```

### Valhalla (Calcul d'Itinéraires)

L'adaptateur `ValhallaAdapter` calcule les meilleures routes :

```java
ValhallaAdapter.RouteRequest request = new RouteAdapter.RouteRequest(coordinates);
ValhallaAdapter.RouteResponse response = valhallaAdapter.calculateRoute(request);
```

### Clerk (Authentification)

JWT validation automatique via Spring Security :
```java
@PreAuthorize("hasRole('DRIVER')")
public ResponseEntity<SubProgramDTO> getSubProgram(@PathVariable Long id) { ... }
```

### Firebase Cloud Messaging

Notifications push configurées via `NotificationService` :

```java
notificationService.notifyClientArrivingSoon(client, driverName, 5);
notificationService.notifyDriverAboutRoute(driver, "Itinéraire créé", "30 commandes");
```

## 📊 Base de Données

### Modèle Entités

```
User (classe parente)
├── Manager
├── Driver
└── Client

Vehicle
├─ Affecté aux Drivers (ManyToMany)
└─ Affecté aux Managers (ManyToOne)

Order (avec PostGIS Point pour livraison)
├─ Client (ManyToOne)
├─ DeliveryProgram (ManyToOne)
└─ SubProgram (ManyToOne)

DeliveryProgram
├─ Orders (OneToMany)
└─ SubPrograms (OneToMany)

SubProgram (=Tournée)
├─ Driver (ManyToOne) - Affecté à 1 conducteur
├─ Vehicle (ManyToOne) - Affecté à 1 véhicule
└─ Orders (OneToMany) - Commandes à livrer

Indices PostGIS :
- driver.current_location (geography)
- order.delivery_location (geography)
```

## 🔐 Sécurité

### Authentification

1. **Clerk génère un JWT**
2. **Client envoie le JWT dans le header Authorization**
3. **Backend valide via Spring Security OAuth2**
4. **Synchronisation de l'utilisateur en base**

### Autorisation (par Rôle)

```java
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public List<DeliveryProgramDTO> getPrograms() { ... }

@PreAuthorize("hasRole('DRIVER')")
public void updateLocation(DriverLocationUpdateDTO dto) { ... }

@PreAuthorize("hasRole('CLIENT')")
public void approveOrder(Long orderId) { ... }
```

## 🌐 API REST - Endpoints Principaux

### Commandes
```
GET    /api/orders                    # Lister les commandes
POST   /api/orders                    # Créer une commande
GET    /api/orders/{id}               # Détail d'une commande
PUT    /api/orders/{id}/approve       # Approuver une livraison
```

### Programmes de Livraison
```
POST   /api/delivery-programs         # Créer et optimiser
GET    /api/delivery-programs/{id}    # Détail d'un programme
```

### Conducteurs
```
PUT    /api/drivers/{id}/location     # Mettre à jour position
GET    /api/drivers/{id}/sub-program  # Sous-programme en cours
```

## 📡 WebSocket STOMP

### Connexion
```
ws://localhost:8080/api/ws
```

### Topics

```
# Recevoir les positions des conducteurs (pour les clients)
/topic/driver-locations

# Recevoir les notifications
/topic/notifications
```

### Envoi de Position (Conducteur)

```
POST /app/driver-location
{
  "driverId": 123,
  "latitude": 33.9716,
  "longitude": -6.8498,
  "timestamp": 1234567890
}
```

## 🧪 Tests

```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Couverture de code
mvn clean test jacoco:report
```

## 📚 Documentation API

### Swagger/OpenAPI

La documentation interactive est disponible à :
```
http://localhost:8080/api/swagger-ui.html
```

## 🔧 Configuration

### Variables d'Environnement (Production)

```bash
# Base de données
DB_URL=jdbc:postgresql://db-host:5432/smartfleet_db
DB_USERNAME=smartfleet_user
DB_PASSWORD=<strong-password>

# Clerk
CLERK_ISSUER=https://your-clerk-domain
CLERK_AUDIENCE=https://your-clerk-domain
JWT_SECRET=<strong-secret>
JWT_EXPIRATION=86400000

# Valhalla
VALHALLA_SERVICE_URL=https://valhalla-api.example.com

# Firebase
FIREBASE_CONFIG_PATH=/path/to/firebase-config.json

# CORS
ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com

# SSL
SSL_ENABLED=true
SSL_KEYSTORE_PATH=/path/to/keystore.p12
SSL_KEYSTORE_PASSWORD=<password>
```

## 📈 Métriques et Monitoring

L'application expose les métriques Actuator :

```
http://localhost:8080/api/actuator/health
http://localhost:8080/api/actuator/metrics
```

## 🐛 Troubleshooting

### Erreur PostGIS
```
ERROR: could not open extension control file "/usr/share/postgresql/extension/postgis.control"
```
**Solution:** Installer PostGIS package `postgresql-16-postgis`

### Valhalla non accessible
```
ERROR: Failed to connect to Valhalla
```
**Solution:** Vérifier que Valhalla est en cours d'exécution sur le port 8002

### Erreur Clerk JWT
```
ERROR: Invalid JWT token
```
**Solution:** Vérifier les valeurs `CLERK_ISSUER` et `CLERK_AUDIENCE`

## 📞 Support et Contribution

- **Issues:** Signaler les bugs sur GitHub
- **Pull Requests:** Les contributions sont bienvenues
- **Email:** support@smartfleet.com

## 📄 License

Ce projet est sous licence MIT. Voir [LICENSE](LICENSE) pour plus de détails.

## 🙏 Remerciements

- [Spring Boot](https://spring.io/projects/spring-boot)
- [OR-Tools](https://developers.google.com/optimization)
- [Valhalla](https://valhalla.readthedocs.io/)
- [Firebase](https://firebase.google.com/)
- [Clerk](https://clerk.com/)
- [PostGIS](https://postgis.net/)

---

**Version:** 1.0.0  
**Dernière mise à jour:** 2026-05-19  
**Auteur:** SmartFleet Development Team

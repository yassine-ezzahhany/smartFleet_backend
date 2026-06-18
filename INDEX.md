# 📑 SmartFleet Backend - Index & Navigation

## 🎯 Commencer Ici

### Pour un Démarrage Rapide
👉 **[QUICK_START.md](QUICK_START.md)** (5 minutes)

### Pour Comprendre l'Architecture
👉 **[ARCHITECTURE.md](ARCHITECTURE.md)** (Complet)

### Pour Implémenter les Services
👉 **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)** (Détaillé avec code)

### Pour un Récapitulatif Complet
👉 **[DELIVERABLES.md](DELIVERABLES.md)** (Vue d'ensemble)

---

## 📂 Structure des Fichiers

### Documentation
```
/README.md                    ← Documentation complète
/QUICK_START.md              ← Démarrage rapide 5 min
/ARCHITECTURE.md             ← Architecture en détail
/IMPLEMENTATION_GUIDE.md      ← Guide d'implémentation avec code
/DELIVERABLES.md             ← Récapitulatif des livrables
/INDEX.md                    ← Ce fichier (navigation)
```

### Configuration
```
/pom.xml                                    ← Dépendances Maven
/docker-compose.yml                         ← Services externes (Docker)
/src/main/resources/application.properties  ← Config par défaut
/src/main/resources/application-dev.properties     ← Config développement
/src/main/resources/application-prod.properties    ← Config production
/src/main/resources/firebase-config-example.json   ← Exemple Firebase
```

### Code Source
```
/src/main/java/ma/smartfleet/backend/
├── config/                             # Configuration Spring (Sécurité, WebSockets, etc.)
├── controller/                         # Contrôleurs REST (Couche API/Présentation)
├── dto/                                # Data Transfer Objects
├── exception/                          # Gestionnaires d'exceptions personnalisées
├── infrastructure/                     # Intégrations (OR-Tools, client Valhalla)
│   └── adapter/                        # Adaptateurs externes
│       └── ORToolsAdapter.java         # Adaptateur OR-Tools
├── model/                              # Entités JPA et énumérations (Base de données)
│   └── enums/                          # Énumérations (UserRole, OrderStatus, etc.)
├── repository/                         # Repositories Spring Data JPA
├── service/                            # Services métier (Logique d'application)
├── util/                               # Utilitaires (JwtTokenProvider, etc.)
└── BackendApplication.java             # Classe principale
```

---

## 🔑 Fichiers Clés par Use Case

### Authentification (Base de Données locale + JWT)
- **Controller:** `controller/AuthController.java`
- **Service:** `service/UserService.java`
- **Security:** `config/SecurityConfig.java`, `config/JwtAuthenticationFilter.java`
- **Entité:** `model/User.java` (avec mot de passe encodé)
- **Repository:** `repository/UserRepository.java`
- **DTO:** `dto/UserDTO.java`, `dto/RegisterRequestDTO.java`, `dto/LoginRequestDTO.java`

### Optimisation Tournées (OR-Tools)
- **Service:** `service/DeliveryOptimizationService.java`
- **Adaptateur:** `infrastructure/adapter/ORToolsAdapter.java`
- **Entités:** `model/DeliveryProgram.java`, `model/SubProgram.java`
- **Repositories:** `repository/DeliveryProgramRepository.java`, `repository/SubProgramRepository.java`

### Calcul d'Itinéraires (Valhalla)
- **Service:** `service/RouteService.java`
- **Client Valhalla:** `service/ValhallaClient.java`
- **Entité:** `model/SubProgram.java` (polyline)

### Traçabilité Temps Réel (WebSocket)
- **Service:** `service/LocationTrackingService.java`
- **Entité:** `model/Driver.java` (currentLocation PostGIS)
- **DTO:** `dto/DriverLocationUpdateDTO.java`
- **Repository:** `repository/DriverRepository.java`

### Approbation Livraison & Gestion Commandes
- **Service:** `service/OrderService.java`
- **Entités:** `model/Order.java`, `model/SubProgram.java`
- **Repositories:** `repository/OrderRepository.java`, `repository/SubProgramRepository.java`

### Notifications Push (Firebase)
- **Service:** `service/NotificationService.java`
- **Configuration:** `src/main/resources/firebase-config.json`

---

## 🛠️ Configuration et Setup

### Variables Requises
Voir `application-prod.properties` pour les variables d'environnement:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION`
- `VALHALLA_SERVICE_URL`
- `FIREBASE_CONFIG_PATH`
- `ALLOWED_ORIGINS`, `SSL_ENABLED`, etc.

### Services Externes (Docker)
```bash
# Voir docker-compose.yml
docker-compose up -d
```
- PostgreSQL 16 + PostGIS
- Valhalla routing engine
- Redis (optionnel)
- pgAdmin (optionnel, profil dev)

---

## 📊 Flux d'Implémentation Recommandé

### Semaine 1-2: Services de Base
1. `SecurityConfig` - Configuration Spring Security et Filtre JWT
2. `OrderManagementServiceImpl` - Gestion commandes
3. Tests + DTOs/Mappers

### Semaine 3: Intégrations Externes
1. `DeliveryOptimizationServiceImpl` - OR-Tools
2. `RouteCalculationServiceImpl` - Valhalla
3. Configuration adapters

### Semaine 4: Temps Réel + Notifications
1. `LocationTrackingServiceImpl` - GPS tracking
2. `NotificationServiceImpl` - Firebase
3. WebSocket handlers STOMP

### Semaine 5: Présentation
1. Contrôleurs REST
2. Global exception handlers
3. OpenAPI/Swagger docs

### Semaine 6-8: Tests + Déploiement
1. Tests unitaires (> 80%)
2. Tests d'intégration
3. Déploiement production

---

## 🚀 Commandes Essentielles

```bash
# Build
mvn clean install

# Démarrage dev
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Tests
mvn test

# Docker
docker-compose up -d
docker-compose logs -f postgres

# PostgreSQL
psql -h localhost -U smartfleet_user -d smartfleet_db_dev
CREATE EXTENSION postgis;
```

---

## 📞 Points de Contact

### Entités
- **Utilisateurs:** User (User.java), Manager, Driver, Client
- **Véhicules:** Vehicle.java
- **Livraisons:** Order, DeliveryProgram, SubProgram

### Services
- Implémentés sous `service/` et `infrastructure/adapter/`

### API & WebSockets
- Implémentés sous `controller/`

### Configuration
- **Base:** `application.properties`
- **Dev:** `application-dev.properties`
- **Prod:** `application-prod.properties`

---

## ✅ Checklist d'Implémentation

- [x] Implémentation des services (`service/`)
- [x] Contrôleurs REST et Endpoints (`controller/`)
- [x] Configuration Security (`config/`)
- [x] Tests unitaires et d'intégration
- [x] Documentation OpenAPI/Swagger
- [ ] Déploiement production

---

**Pour plus de détails → Voir les fichiers .md**

**Architecture Status:** ✅ COMPLETE  
**Implementation Status:** 0% (prêt à implémenter)

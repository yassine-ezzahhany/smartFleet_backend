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
├── BackendApplication.java              ← Classe principale
├── domain/
│   ├── model/                          ← Entités JPA
│   │   ├── User.java                   ← Classe parente utilisateurs
│   │   ├── Manager.java
│   │   ├── Driver.java                 ← Avec PostGIS Location
│   │   ├── Client.java                 ← Avec Firebase token
│   │   ├── Vehicle.java                ← Avec capacités m2/kg
│   │   ├── Order.java                  ← Avec PostGIS Point
│   │   ├── DeliveryProgram.java
│   │   ├── SubProgram.java             ← Tournée (1 driver + 1 vehicle)
│   │   └── enums/
│   │       ├── UserRole.java
│   │       ├── OrderStatus.java
│   │       ├── DeliveryProgramStatus.java
│   │       └── SubProgramStatus.java
│   ├── service/                        ← Interfaces de services métier
│   │   ├── DeliveryOptimizationService.java
│   │   ├── RouteCalculationService.java
│   │   ├── LocationTrackingService.java
│   │   ├── OrderManagementService.java
│   │   ├── NotificationService.java
│   │   ├── AuthenticationService.java
│   │   └── OptimizationStats.java
│   └── exception/
│       ├── SmartFleetException.java
│       ├── ResourceNotFoundException.java
│       ├── OptimizationException.java
│       └── ValhallaServiceException.java
├── application/                        ← À implémenter
│   ├── service/impl/                   ← Implémentations services (NEXT)
│   ├── dto/                            ← DTOs API
│   │   ├── UserDTO.java
│   │   ├── OrderDTO.java
│   │   ├── DeliveryProgramDTO.java
│   │   ├── SubProgramDTO.java
│   │   ├── DriverLocationUpdateDTO.java
│   │   └── OrderApprovalDTO.java
│   └── mapper/                         ← Mappers (DTOs ↔ Entités)
├── infrastructure/                     ← Détails techniques
│   ├── adapter/                        ← Adaptateurs externes
│   │   ├── ORToolsAdapter.java         ← VRP interface
│   │   └── ValhallaAdapter.java        ← Route optimization interface
│   ├── config/                         ← Configuration Spring (NEXT)
│   ├── persistence/                    ← Repositories JPA
│   │   ├── UserRepository.java
│   │   ├── ManagerRepository.java
│   │   ├── DriverRepository.java       ← Avec queries PostGIS
│   │   ├── ClientRepository.java
│   │   ├── VehicleRepository.java
│   │   ├── OrderRepository.java        ← Avec queries PostGIS
│   │   ├── DeliveryProgramRepository.java
│   │   └── SubProgramRepository.java
│   └── rest/                           ← Clients HTTP (NEXT)
├── presentation/                       ← À implémenter
│   ├── controller/                     ← Endpoints REST (NEXT)
│   └── websocket/                      ← WebSocket STOMP (NEXT)
└── shared/
    ├── constants/
    │   └── AppConstants.java           ← Constantes centralisées
    └── utility/                        ← Utilitaires (NEXT)
```

---

## 🔑 Fichiers Clés par Use Case

### Authentification (Clerk JWT)
- **Interface:** `domain/service/AuthenticationService.java`
- **Entité:** `domain/model/User.java`, `Manager.java`, `Driver.java`, `Client.java`
- **Repository:** `infrastructure/persistence/UserRepository.java`
- **DTO:** `application/dto/UserDTO.java`
- **À implémenter:** `application/service/impl/AuthenticationServiceImpl.java`

### Optimisation Tournées (OR-Tools)
- **Interface:** `domain/service/DeliveryOptimizationService.java`
- **Adaptateur:** `infrastructure/adapter/ORToolsAdapter.java`
- **Entités:** `domain/model/DeliveryProgram.java`, `SubProgram.java`
- **Repositories:** `infrastructure/persistence/DeliveryProgramRepository.java`, `SubProgramRepository.java`
- **À implémenter:** `application/service/impl/DeliveryOptimizationServiceImpl.java`

### Calcul d'Itinéraires (Valhalla)
- **Interface:** `domain/service/RouteCalculationService.java`
- **Adaptateur:** `infrastructure/adapter/ValhallaAdapter.java`
- **Entité:** `domain/model/SubProgram.java` (polyline)
- **À implémenter:** `application/service/impl/RouteCalculationServiceImpl.java`

### Traçabilité Temps Réel (WebSocket)
- **Interface:** `domain/service/LocationTrackingService.java`
- **Entité:** `domain/model/Driver.java` (currentLocation PostGIS)
- **DTO:** `application/dto/DriverLocationUpdateDTO.java`
- **Repository:** `infrastructure/persistence/DriverRepository.java` (findDriversNearby)
- **À implémenter:** `application/service/impl/LocationTrackingServiceImpl.java`
- **À implémenter:** `presentation/websocket/LocationWebSocketHandler.java`

### Approbation Livraison
- **Interface:** `domain/service/OrderManagementService.java`
- **Entités:** `domain/model/Order.java`, `SubProgram.java`
- **Repositories:** `infrastructure/persistence/OrderRepository.java`, `SubProgramRepository.java`
- **DTO:** `application/dto/OrderApprovalDTO.java`
- **À implémenter:** `application/service/impl/OrderManagementServiceImpl.java`

### Notifications Push (Firebase)
- **Interface:** `domain/service/NotificationService.java`
- **Configuration:** `src/main/resources/firebase-config.json`
- **À implémenter:** `application/service/impl/NotificationServiceImpl.java`

---

## 🛠️ Configuration et Setup

### Variables Requises
Voir `application-prod.properties` pour les variables d'environnement:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `CLERK_ISSUER`, `CLERK_AUDIENCE`, `JWT_SECRET`
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
1. `AuthenticationServiceImpl` - Intégration Clerk
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
- **Tous** implémentés en: `infrastructure/adapter/` + `application/service/impl/`

### API
- **À créer:** `presentation/controller/`
- **WebSocket:** `presentation/websocket/`

### Configuration
- **Base:** `application.properties`
- **Dev:** `application-dev.properties`
- **Prod:** `application-prod.properties`

---

## ✅ Checklist d'Implémentation

- [ ] Service implementations (application/service/impl/)
- [ ] Mappers (application/mapper/)
- [ ] Contrôleurs REST (presentation/controller/)
- [ ] WebSocket STOMP handlers (presentation/websocket/)
- [ ] Global exception handler
- [ ] Configuration Security (infrastructure/config/)
- [ ] Tests unitaires
- [ ] Tests d'intégration
- [ ] OpenAPI/Swagger docs
- [ ] Déploiement production

---

**Pour plus de détails → Voir les fichiers .md**

**Architecture Status:** ✅ COMPLETE  
**Implementation Status:** 0% (prêt à implémenter)

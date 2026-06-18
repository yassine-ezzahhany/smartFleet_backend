# 📦 SmartFleet Backend - Livrables

## ✅ Résumé des Livrables

Ce document récapitule tous les éléments fournis pour le backend SmartFleet, une application de gestion logistique complète.

---

## 1️⃣ Structure du Projet

### Structure des Packages (Architecture Multiniveau)

```
backend/src/main/java/ma/smartfleet/backend/
├── config/                   # Configuration Spring (Sécurité, WebSockets, etc.)
├── controller/               # Endpoints REST (Couche API)
├── dto/                      # Data Transfer Objects
├── exception/                # Exceptions personnalisées et gestionnaires d'erreurs
├── infrastructure/           # Intégrations (OR-Tools, client Valhalla)
│   └── adapter/              # Adaptateurs (ORToolsAdapter)
├── model/                    # Entités JPA et Enums
│   └── enums/                # Énumérations (UserRole, OrderStatus, etc.)
├── repository/               # Repositories JPA
├── service/                  # Services métier (Logique d'application)
└── util/                     # Utilitaires (Générateur JWT, etc.)
```

---

## 2️⃣ Entités JPA (Domain Model)

### Hiérarchie d'Utilisateurs

| Entité | Description |
|--------|-------------|
| **User** | Classe parente (abstract) avec authentification locale |
| **Manager** | Responsable de turnées de livraison |
| **Driver** | Conducteur avec suivi GPS en temps réel |
| **Client** | Client/Expéditeur avec notifications Firebase |

### Entités de Livraison

| Entité | Description | Points Clés |
|--------|-------------|------------|
| **Vehicle** | Véhicule de livraison | Capacité m2 et kg |
| **Order** | Commande de livraison | Coordonnées PostGIS, statut |
| **DeliveryProgram** | Programme regroupant commandes | Statut: PENDING → COMPLETED |
| **SubProgram** | Tournée (1 driver + 1 vehicle) | Polyline Valhalla, itinéraire |

### Énumérations

```
UserRole: ADMIN, MANAGER, DRIVER, CLIENT
OrderStatus: PENDING, ASSIGNED, IN_TRANSIT, ARRIVING_SOON, DELIVERED, REJECTED, CANCELLED
DeliveryProgramStatus: PENDING, OPTIMIZED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
SubProgramStatus: PENDING, ASSIGNED, IN_TRANSIT, COMPLETED, FAILED, CANCELLED
```

### Indices PostGIS

```sql
driver.current_location → geography(POINT,4326)
order.delivery_location → geography(POINT,4326)
```

---

## 3️⃣ Interfaces de Services (Domain Layer)

### Services Métier

| Service | Responsabilité |
|---------|-----------------|
| **DeliveryOptimizationService** | Optimisation des tournées via OR-Tools |
| **RouteCalculationService** | Calcul d'itinéraires via Valhalla |
| **LocationTrackingService** | Suivi GPS et géolocalisation |
| **OrderManagementService** | Gestion du cycle de vie des commandes |
| **NotificationService** | Notifications push Firebase |
| **UserService** | Gestion des utilisateurs et de l'enregistrement |

### Méthodes Clés

**DeliveryOptimizationService**
```java
DeliveryProgram optimizeDeliveryProgram(DeliveryProgram program)
boolean canOptimizeOrders(List<Order> orders)
OptimizationStats getOptimizationStats(Long programId)
```

**RouteCalculationService**
```java
SubProgram calculateOptimalRoute(SubProgram subProgram)
RouteMetrics calculateRouteMetrics(List<LatLng> coordinates)
boolean isValidRoute(List<LatLng> coordinates)
```

**LocationTrackingService**
```java
void updateDriverLocation(Long driverId, Double lat, Double lng, Long timestamp)
Point getDriverLastLocation(Long driverId)
Double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2)
boolean isDriverNearby(Long driverId, Double delLat, Double delLng, Double radiusKm)
```

**OrderManagementService**
```java
Order approveOrderDelivery(Long orderId)
boolean checkAndUpdateSubProgramCompletion(Long subProgramId)
Order rejectOrder(Long orderId, String reason)
void createOrderTrackingRecord(Long orderId, String status, String notes)
```

---

## 4️⃣ Adaptateurs pour Services Externes

### ORToolsAdapter (Vehicle Routing Problem)

**Classe Input**
```java
OptimizationResult optimizeDeliveries(
    List<Order> orders,           // Commandes à livrer
    List<Vehicle> vehicles,       // Véhicules disponibles
    List<Driver> drivers          // Conducteurs disponibles
)
```

**Classes de Sortie**
- `RouteSolution`: Route pour un véhicule/conducteur
  - `vehicleId`, `driverId`, `orderIds`
  - `routeDistance`, `routeTime`
  - `loadedWeightKg`, `loadedVolumeM2`

- `OptimizationResult`: Résultat global
  - `List<RouteSolution> routes`
  - `totalDistance`, `totalTime`

- `OptimizationConfig`: Contraintes
  - `maxTimeLimit`, `maxWeightPerVehicle`, `maxVolumePerVehicle`
  - `maxOrdersPerRoute`, `depot coordinates`

### ValhallaAdapter (Route Optimization)

**Méthodes**
```java
RouteResponse calculateRoute(RouteRequest request)        // Route simple
RouteResponse optimizeRoute(Double lat, Double lon, 
                           List<Coordinate> coords)       // Route optimisée
double[][] calculateDistanceMatrix(List<Integer> sources,
                                   List<Integer> targets,
                                   List<Coordinate> coords) // Matrice distances
```

**Classes**
- `Coordinate`: { lon, lat }
- `Route`: { geometry (polyline), distance (m), duration (s) }
- `Waypoint`: { lat, lon, name }

---

## 5️⃣ Repositories (Persistence Layer)

| Repository | Entité |
|------------|--------|
| `UserRepository` | User |
| `ManagerRepository` | Manager |
| `DriverRepository` | Driver (avec requêtes géospatiales) |
| `ClientRepository` | Client |
| `VehicleRepository` | Vehicle |
| `OrderRepository` | Order (avec requêtes géospatiales) |
| `DeliveryProgramRepository` | DeliveryProgram |
| `SubProgramRepository` | SubProgram |

**Requêtes Spécialisées**
```java
// DriverRepository
List<Driver> findDriversNearby(Double lon, Double lat, Double radiusMeters)

// OrderRepository
List<Order> findOrdersNearby(Double lon, Double lat, Double radiusMeters)
```

---

## 6️⃣ DTOs (Application Layer)

| DTO | Usage |
|-----|-------|
| `UserDTO` | Représentation utilisateur |
| `OrderDTO` | Représentation commande |
| `DeliveryProgramDTO` | Représentation programme |
| `SubProgramDTO` | Représentation tournée |
| `DriverLocationUpdateDTO` | Mise à jour position en temps réel |
| `OrderApprovalDTO` | Approbation de livraison |

---

## 7️⃣ Configuration Spring

### pom.xml

**Dépendances Principales**
- Spring Boot 3.3.5
- Spring Data JPA + Hibernate Spatial
- Spring Security + OAuth2 Resource Server
- Spring WebSocket STOMP
- PostgreSQL Driver + PostGIS JTS
- OR-Tools 9.9.3963
- Firebase Admin SDK 9.2.0
- JWT (JJWT) 0.12.3
- MapStruct 1.6.3
- Lombok

### application.properties

```properties
# PostgreSQL + PostGIS
spring.datasource.url=jdbc:postgresql://localhost:5432/smartfleet_db_dev
spring.jpa.database-platform=org.hibernate.spatial.dialect.postgis.PostgisDialect

# Security & JWT
app.security.jwt.secret=...
app.security.jwt.expiration-ms=86400000

# Valhalla
valhalla.service.url=http://localhost:8002

# OR-Tools
ortools.optimization.time-limit-seconds=60

# Firebase
firebase.notification.enabled=true

# WebSocket
spring.websocket.stomp.enabled=true

# Location Tracking
location.tracking.update-frequency-seconds=30
location.tracking.broadcast-frequency-minutes=2
location.tracking.nearby-radius-km=1.0
```

### Profiles

- **dev** (application-dev.properties): Configuration développement
- **prod** (application-prod.properties): Configuration production avec variables d'env

---

## 8️⃣ Configuration de Sécurité

### Authentification (Base de Données locale + JWT)

```java
// Spring Security avec filtre d'authentification personnalisé
@EnableWebSecurity
public class SecurityConfig {
    // Validation des JWT locaux via JwtAuthenticationFilter
    // Gestion des sessions stateless
}
```

### Autorisation (par Rôle)

```java
@PreAuthorize("hasRole('MANAGER')")      // Manager
@PreAuthorize("hasRole('DRIVER')")       // Conducteur
@PreAuthorize("hasRole('CLIENT')")       // Client
@PreAuthorize("hasRole('ADMIN')")        // Admin
```

### CORS

Configuré pour domaines: `http://localhost:3000`, `http://localhost:3001`

---

## 9️⃣ WebSocket STOMP

### Topics

```
/topic/driver-locations        # Positions conducteurs
/topic/notifications           # Notifications générales
```

### Endpoints

```
ws://localhost:8080/api/ws
POST /app/driver-location      # Envoyer position
```

### Limitation de Fréquence

- Conducteur envoie position: **30 secondes**
- Clients reçoivent broadcast: **max 2 minutes**

---

## 🔟 Constantes Applicatives (AppConstants.java)

```java
// WebSocket
WS_ENDPOINT = "/ws"
WS_DRIVER_LOCATION_TOPIC = "/topic/driver-locations"

// Location Tracking
DRIVER_LOCATION_UPDATE_FREQUENCY_SECONDS = 30
CLIENT_LOCATION_BROADCAST_FREQUENCY_MINUTES = 2
DRIVER_NEARBY_RADIUS_KM = 1.0

// Optimization
MAX_TIME_LIMIT_SECONDS = 28800 (8 heures)
MAX_ORDERS_PER_ROUTE = 50
OPTIMIZATION_TIME_LIMIT_SECONDS = 60

// Firebase
FIREBASE_TITLE_ROUTE = "Votre itinéraire a été créé"
FIREBASE_TITLE_ARRIVING = "Arrivée imminente"
FIREBASE_TITLE_COMPLETE = "Tournée terminée"
```

---

## 📚 Documentation Fournie

| Document | Contenu |
|----------|---------|
| **ARCHITECTURE.md** | Architecture multiniveau, flux métier, entités |
| **README.md** | Quick start, dépendances, déploiement |
| **IMPLEMENTATION_GUIDE.md** | Guide détaillé pour implémenter les services |
| **DELIVERABLES.md** | Ce document (récapitulatif) |

---

## 🛠️ Fichiers de Configuration

| Fichier | Usage |
|---------|-------|
| `docker-compose.yml` | Services externes (PostgreSQL, Valhalla, Redis) |
| `firebase-config-example.json` | Exemple config Firebase |
| `application.properties` | Config par défaut |
| `application-dev.properties` | Config développement |
| `application-prod.properties` | Config production |

---

## 🚀 Services Externes (Docker Compose)

```yaml
- PostgreSQL 16 + PostGIS  (port 5432)
- Valhalla                  (port 8002)
- Redis                     (port 6379, optionnel)
- pgAdmin                   (port 5050, dev uniquement)
```

**Démarrage**
```bash
docker-compose up -d
```

---

## 📊 Flux Métier Complets

### 1. Création et Optimisation de Programme

```
Créer Programme → Ajouter Commandes → OR-Tools
→ Crée Sous-programmes (1 par conducteur/véhicule)
→ Status: PENDING → OPTIMIZED
```

### 2. Calcul d'Itinéraires

```
Sous-programme → Valhalla → Polyline + Métriques
→ Status: PENDING → ASSIGNED → IN_TRANSIT
```

### 3. Traçabilité Temps Réel

```
Driver → Position (30s) → WebSocket → Backend
→ Broadcast aux clients (max 2 min)
```

### 4. Approbation Livraison

```
Client approuve → Order.clientApproved = true
→ Vérifier tous les ordres du SubProgram
→ Si tout approuvé → SubProgram.status = COMPLETED
```

---

## 🔐 Sécurité Implémentée

✅ Authentification JWT locale (BD + Spring Security)  
✅ Autorisation par rôles (RBAC)  
✅ CORS configuré  
✅ Validation des entrées  
✅ Gestion des exceptions  
✅ Logs de sécurité  

---

## 🎯 À Implémenter (Next Steps)

### Phase 1: Services Métier
- [ ] AuthenticationServiceImpl
- [ ] OrderManagementServiceImpl
- [ ] OptimizationStatsImpl

### Phase 2: Intégrations Externes
- [ ] DeliveryOptimizationServiceImpl (OR-Tools)
- [ ] RouteCalculationServiceImpl (Valhalla)
- [ ] LocationTrackingServiceImpl
- [ ] NotificationServiceImpl (Firebase)

### Phase 3: Présentation
- [ ] Contrôleurs REST (OrderController, etc.)
- [ ] WebSocket STOMP handlers
- [ ] Global Exception Handler

### Phase 4: Tests
- [ ] Tests unitaires des services
- [ ] Tests d'intégration
- [ ] Tests d'API (Postman/REST Client)

### Phase 5: Documentation
- [ ] OpenAPI/Swagger
- [ ] Postman Collection
- [ ] DeploymentGuide

---

## 📈 Métriques et KPIs

**Backend Performance**
- Latence d'optimisation: < 1 minute pour 1000 commandes
- Latence calcul itinéraire: < 5 secondes
- Latence mise à jour position: < 100ms

**Qualité Code**
- Couverture tests: > 80%
- Code duplication: < 3%
- Cyclomatic complexity: < 10

---

## 📝 Notes Importantes

1. **PostGIS Setup**: Extension PostGIS doit être installée sur PostgreSQL
2. **OR-Tools Native**: Bibliothèque native requise pour le système d'exploitation
3. **Valhalla**: Service peut être auto-hébergé ou cloud (provider)
4. **Firebase**: Configuration JSON requise pour notifications push
5. **JWT** : Configuration de la clé secrète JWT essentielle pour l'authentification localisée

---

## 📞 Support

Pour tout question ou problème:
- Consulter [ARCHITECTURE.md](ARCHITECTURE.md)
- Consulter [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- Voir les issues sur le repository

---

**Version:** 1.0.0  
**Date:** 2026-05-19  
**Status:** ✅ Architecture Complète - Prêt pour Implémentation  
**Temps Estimé d'Implémentation:** 6-8 semaines

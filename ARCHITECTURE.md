# SmartFleet Backend - Documentation Architecturale

## 📋 Vue d'ensemble

SmartFleet est une application backend de gestion logistique utilisant une **architecture multiniveau (N-Tier/layered architecture)** avec Spring Boot 3, PostgreSQL et PostGIS pour optimiser les tournées de livraison.

## 🏗️ Architecture du Projet

### Structure des Packages

```
ma.smartfleet.backend/
├── config/                         # Configuration Spring (Sécurité JWT, WebSocket, Swagger, etc.)
├── controller/                     # Contrôleurs REST (Couche Présentation)
├── dto/                            # Data Transfer Objects
├── exception/                      # Gestionnaires d'exceptions personnalisées
├── infrastructure/                 # Intégrations et Clients (OR-Tools, client Valhalla)
│   └── adapter/
│       └── ORToolsAdapter.java     # Adaptateur OR-Tools
├── model/                          # Entités JPA de la Base de Données et énumérations
│   └── enums/                      # Enums (UserRole, OrderStatus, etc.)
├── repository/                     # Interfaces repositories JPA (Accès Données)
├── service/                        # Logique métier (Services)
└── util/                           # Utilitaires (Générateur JWT, etc.)
```

## 🗄️ Architecture de la Base de Données

### Tables Principales

**users** (Inheritance - JOINED strategy)
- Classe parente avec les informations communes
- Utilise l'héritage JPA pour les rôles

**managers**
- Responsables de tournées

**drivers**
- Conducteurs avec positions GPS (PostGIS Point)

**clients**
- Clients/Expéditeurs avec tokens Firebase

**vehicles**
- Véhicules avec capacités (m2, kg)

**orders**
- Commandes avec coordonnées GPS (PostGIS Point)

**delivery_programs**
- Programmes de livraison regroupant les commandes

**sub_programs**
- Sous-programmes (une tournée = 1 conducteur + 1 véhicule + commandes)

## 🔐 Intégrations Principales

### 1. **Authentification (Base de Données locale + JWT)**
```
Client (Email/Password) → Authentification /auth/login → Backend vérifie et génère JWT → Client stocke JWT
Requête suivante → Header Authorization (Bearer <JWT>) → Spring Security valide le JWT localement → Détermine rôle
```
- Authentification locale par mot de passe encodé (BCrypt) via `/auth/login` ou `/auth/register`
- Utilisation de `JwtTokenProvider` et `JwtAuthenticationFilter` pour valider le JWT à chaque requête
- Gestion de la session stateless dans Spring Security

### 2. **Optimisation (OR-Tools)**
```
Commandes + Véhicules + Conducteurs → OR-Tools → Routes optimisées
```
- Vehicle Routing Problem (VRP)
- Contraintes: capacité, poids, volume
- Regroupement par conducteur/véhicule

### 3. **Calcul d'Itinéraires (Valhalla)**
```
Points de livraison → Valhalla → Polylines + Distances + Durées
```
- Calcul de la meilleure route
- Polylines encodées
- Matrice de distances

### 4. **Temps Réel (WebSocket STOMP)**
```
Driver → Position (30s) → Backend → Diffuse clients (max 2 min)
```
- Connexion persistante par conducteur
- Limitation de fréquence pour éviter surcharge

### 5. **Notifications (Firebase Cloud Messaging)**
```
Events → FCM → Notifications push conducteurs et clients
```
- Itinéraire créé
- Arrivée imminente
- Tournée terminée

## 🎯 Flux Métier Principal

### 1. Création de Programme de Livraison
```
Commandes → Validation → Optimisation OR-Tools → Sous-programmes créés
```

### 2. Calcul d'Itinéraires
```
Sous-programme → Valhalla → Polylines + Métriques → Base de données
```

### 3. Traçabilité en Temps Réel
```
Driver → Envoie position (30s) → Backend → Diffuse clients (max 2 min)
```

### 4. Approbation de Livraison
```
Client approuve → Mise à jour statut commande → Vérification sous-programme
→ Si tout approuvé, sous-programme = COMPLETED
```

## 📡 Entités JPA Principales

### User (Classe Parente)
```java
- password (mot de passe encodé BCrypt)
- email (unique)
- firstName
- lastName
- phone
- role (ENUM: ADMIN, MANAGER, DRIVER, CLIENT)
- active
```

### Driver (extends User)
```java
- licenseNumber (unique)
- licenseExpiry
- manager (ManyToOne)
- assignedVehicles (ManyToMany)
- currentLocation (PostGIS Point)
- currentLatitude/currentLongitude
- available (boolean)
```

### Order
```java
- orderNumber (unique)
- client (ManyToOne)
- weightKg
- volumeM2
- deliveryLocation (PostGIS Point)
- deliveryLatitude/deliveryLongitude
- status (ENUM)
- subProgram (ManyToOne)
- clientApproved
- clientApprovalTime
```

### SubProgram
```java
- subProgramNumber (unique)
- deliveryProgram (ManyToOne)
- driver (ManyToOne) - Assigné à 1 conducteur
- vehicle (ManyToOne) - Assigné à 1 véhicule
- orders (OneToMany) - Commandes à livrer
- polyline (géométrie Valhalla)
- estimatedDistanceKm
- estimatedDurationMinutes
- status (ENUM)
- totalOrdersCount
- approvedOrdersCount
```

## 🔌 Interfaces de Services

### DeliveryOptimizationService
```java
DeliveryProgram optimizeDeliveryProgram(DeliveryProgram program)
boolean canOptimizeOrders(List<Order> orders)
OptimizationStats getOptimizationStats(Long programId)
```

### RouteCalculationService
```java
SubProgram calculateOptimalRoute(SubProgram subProgram)
RouteMetrics calculateRouteMetrics(List<LatLng> coordinates)
boolean isValidRoute(List<LatLng> coordinates)
```

### LocationTrackingService
```java
void updateDriverLocation(Long driverId, Double lat, Double lng, Long timestamp)
Point getDriverLastLocation(Long driverId)
Double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2)
boolean isDriverNearby(Long driverId, Double delLat, Double delLng, Double radiusKm)
```

### OrderManagementService
```java
Order approveOrderDelivery(Long orderId)
boolean checkAndUpdateSubProgramCompletion(Long subProgramId)
Order rejectOrder(Long orderId, String reason)
void createOrderTrackingRecord(Long orderId, String status, String notes)
```

### NotificationService
```java
void notifyDriverAboutRoute(Driver driver, String title, String body)
void notifyClientArrivingSoon(Client client, String driverName, Integer minutes)
void notifyClientOrderUpdate(Client client, Long orderId, String status)
void notifyDriverProgramComplete(Driver driver, Long subProgramId)
```

## 📊 Adaptateurs Externes

### ORToolsAdapter
Interface pour l'intégration avec OR-Tools pour l'optimisation des tournées.

**Méthodes principales:**
```java
OptimizationResult optimizeDeliveries(List<Order>, List<Vehicle>, List<Driver>)
```

**Classes imbriquées:**
- `RouteSolution`: Solution pour une route (véhicule, conducteur, commandes)
- `OptimizationResult`: Résultat complet d'optimisation
- `OptimizationConfig`: Configuration des contraintes

### ValhallaAdapter
Interface pour l'intégration avec Valhalla pour le calcul d'itinéraires.

**Méthodes principales:**
```java
RouteResponse calculateRoute(RouteRequest request)
RouteResponse optimizeRoute(Double startLat, Double startLon, List<Coordinate> coords)
double[][] calculateDistanceMatrix(List<Integer> sources, List<Integer> targets, List<Coordinate> coords)
```

## 🔄 Constantes Applicatives

Voir `AppConstants.java` pour:
- WebSocket topics et endpoints
- Statuts (ORDER, SUB_PROGRAM, DELIVERY_PROGRAM)
- Limites d'optimisation
- Configuration Firebase
- Configuration Valhalla

## 📦 Dépendances Principales (pom.xml)

```xml
- Spring Boot 3.3.5
- Spring Data JPA
- Spring Security + OAuth2
- Spring WebSocket
- Spring WebFlux (pour Valhalla)
- PostgreSQL Driver
- Hibernate Spatial (PostGIS)
- JTS (Java Topology Suite)
- OR-Tools 9.9.3963
- Firebase Admin SDK 9.2.0
- JWT (JJWT) 0.12.3
- MapStruct 1.6.3
- Lombok
```

## 🚀 Configuration Application

Le fichier `application.properties` contient:
- Configuration PostgreSQL + PostGIS
- Configuration JWT (secret et durée d'expiration)
- Configuration Valhalla
- Configuration OR-Tools
- Configuration Firebase
- Configuration WebSocket
- Configuration CORS
- Configuration tracking en temps réel

## 📝 DTOs Principaux

- `UserDTO`: Représentation utilisateur
- `OrderDTO`: Représentation commande
- `DeliveryProgramDTO`: Représentation programme
- `SubProgramDTO`: Représentation sous-programme
- `DriverLocationUpdateDTO`: Mise à jour position
- `OrderApprovalDTO`: Approbation de livraison

## 🔐 Sécurité

- Spring Security avec OAuth2 Resource Server
- Validation des JWT via filtre de sécurité Spring Security local
- Rôles: ADMIN, MANAGER, DRIVER, CLIENT
- Annotations @PreAuthorize pour autorisation
- CORS configuré pour domaines connus

## 🎓 Prochaines Étapes

1. Implémentation des services de la couche `application/service/`
2. Implémentation des adaptateurs (ORToolsAdapter, ValhallaAdapter)
3. Implémentation des contrôleurs REST
4. Configuration WebSocket STOMP
5. Intégration Firebase
6. Tests unitaires et d'intégration
7. Documentation Swagger/OpenAPI

# SmartFleet Backend — Context (Mis à jour)

## Project Overview
SmartFleet est un backend de gestion logistique construit avec **Spring Boot 3.3.5** et **Java 17**.  
Il gère la planification des livraisons, l'optimisation des tournées, le tracking GPS en temps réel, les notifications push et les accès basés sur les rôles.

---

## Tech Stack

| Composant | Technologie |
|---|---|
| Framework | Spring Boot 3.3.5 |
| Langage | Java 17 |
| Base de données | **Supabase** (PostgreSQL 15 + PostGIS — cloud géré) |
| ORM | Spring Data JPA + Hibernate Spatial |
| Sécurité | Spring Security + Custom stateless JWT (BCrypt, souverain) |
| HTTP Client | Spring WebFlux `WebClient` |
| WebSocket | Spring WebSocket + STOMP (Diffusion temps réel managers & clients) |
| Notifications | Firebase Admin SDK (FCM — stub prêt à câbler) |
| Optimisation VRP | Google OR-Tools (VRP avec double contrainte de capacité) |
| Routage | Valhalla (Docker local opérationnel, carte du Maroc) |
| Mapping | Lombok |

---

## Architecture — Layered (N-Tier)

```
ma.smartfleet.backend
├── BackendApplication.java
├── model/
│   ├── User.java
│   ├── Manager.java
│   ├── Driver.java
│   ├── Client.java
│   ├── Vehicle.java
│   ├── Order.java
│   ├── SubProgram.java
│   ├── DeliveryProgram.java
│   └── enums/
│       ├── UserRole.java
│       ├── OrderStatus.java
│       ├── SubProgramStatus.java
│       └── DeliveryProgramStatus.java
├── repository/
│   ├── UserRepository.java
│   ├── ManagerRepository.java
│   ├── DriverRepository.java
│   ├── ClientRepository.java
│   ├── VehicleRepository.java
│   ├── OrderRepository.java
│   ├── SubProgramRepository.java
│   └── DeliveryProgramRepository.java
├── service/
│   ├── ValhallaClient.java
│   ├── RouteService.java
│   ├── OrderService.java
│   ├── LocationTrackingService.java
│   ├── DeliveryOptimizationService.java
│   ├── VehicleService.java
│   └── NotificationService.java
├── controller/
│   ├── AuthController.java
│   ├── HealthController.java
│   ├── OrderController.java
│   ├── DriverController.java
│   ├── SubProgramController.java
│   ├── VehicleController.java
│   └── OptimizationController.java
├── config/
│   ├── WebClientConfig.java
│   ├── SecurityConfig.java
│   ├── PasswordEncoderConfig.java
│   ├── JwtAuthenticationFilter.java
│   └── WebSocketConfig.java
├── exception/
│   ├── SmartFleetException.java
│   ├── ResourceNotFoundException.java
│   ├── ValhallaServiceException.java
│   ├── OptimizationException.java
│   └── GlobalExceptionHandler.java
├── util/
│   └── JwtTokenProvider.java
└── dto/
    ├── UserDTO.java
    ├── OrderDTO.java
    ├── SubProgramDTO.java
    ├── DeliveryProgramDTO.java
    ├── DriverLocationUpdateDTO.java
    ├── VehicleDTO.java
    ├── LoginRequestDTO.java
    ├── LoginResponseDTO.java
    └── RegisterRequestDTO.java
```

---

## Couche Model (`model/`)

| Entité | Description |
|---|---|
| `User` | Base avec héritage JOINED → `Manager`, `Driver`, `Client` |
| `Manager` | Représentant officiel d'usine/entreprise, gère les chauffeurs et véhicules |
| `Driver` | Chauffeur (manager_id nullable pour self-registration, position PostGIS `Point`) |
| `Client` | Client final avec ses commandes |
| `Vehicle` | Véhicule avec `maxPayloadKg` et `maxVolumeM2` (affecté à un manager) |
| `Order` | Commande avec coordonnées GPS et PostGIS `Point` |
| `SubProgram` | Tournée : 1 chauffeur + 1 véhicule + N commandes |
| `DeliveryProgram` | Programme de livraison groupant les sous-programmes |

---

## Couche Repository (`repository/`)

8 repositories Spring Data JPA. Requêtes notables :
- `DriverRepository.findByManagerIsNull()` — chauffeurs inscrits sans organisation
- `DriverRepository.findByManagerIdAndAvailableTrue()` — chauffeurs dispo d'une usine
- `VehicleRepository.findByManagerIdAndActiveTrue()` — camions actifs d'une usine
- `DriverRepository.findDriversNearby()` — requête PostGIS ST_DWithin
- `OrderRepository.findOrdersNearby()` — commandes proches d'un point GPS

---

## Couche Service (`service/`)

| Service | Responsabilité |
|---|---|
| `ValhallaClient` | Client HTTP pour Valhalla (`/route`, `/optimized_route`, `/matrix`) |
| `RouteService` | Calcule l'itinéraire optimal d'un `SubProgram` via Valhalla |
| `OrderService` | Approbation/rejet de livraisons, complétion automatique des sous-programmes |
| `LocationTrackingService` | Mise à jour GPS, PostGIS, Haversine, diffusion STOMP temps réel |
| `VehicleService` | Ajout, modification physique et gestion de flotte pour Managers |
| `DeliveryOptimizationService` | Planifie et optimise les tournées (`SubProgram`) via Google OR-Tools VRP |
| `NotificationService` | Push FCM vers drivers et clients (log stub, prêt à câbler) |

---

## Couche Controller (`controller/`)

Base path : `/api` (via `server.servlet.context-path=/api`)

| Controller | Méthode | Endpoint | Description |
|---|---|---|---|
| `HealthController` | GET | `/api/health` | Statut de l'application |
| `AuthController` | POST | `/api/auth/register` | Inscription locale souveraine |
| `AuthController` | POST | `/api/auth/login` | Authentification & génération de JWT |
| `AuthController` | GET/PUT | `/api/auth/me` | Gestion du profil utilisateur |
| `OrderController` | GET | `/api/orders/{id}` | Détail d'une commande |
| `OrderController` | POST | `/api/orders/{id}/approve` | Client approuve la livraison |
| `OrderController` | POST | `/api/orders/{id}/reject` | Rejeter une commande |
| `DriverController` | PUT | `/api/drivers/{id}/location` | Mise à jour GPS du chauffeur |
| `DriverController` | GET | `/api/drivers/{id}/nearby` | Vérifie proximité d'un point |
| `DriverController` | GET | `/api/drivers/unassigned` | Liste les chauffeurs disponibles |
| `DriverController` | GET | `/api/drivers/my-drivers` | Liste les chauffeurs recrutés |
| `DriverController` | POST | `/api/drivers/{id}/assign` | Recruter un chauffeur |
| `DriverController` | POST | `/api/drivers/{id}/unassign` | Libérer un chauffeur |
| `VehicleController` | POST | `/api/vehicles` | Enregistrer un véhicule |
| `VehicleController` | GET | `/api/vehicles` | Liste de toute sa flotte |
| `VehicleController` | PUT | `/api/vehicles/{id}` | Modifier les capacités |
| `SubProgramController` | GET | `/api/subprograms/{id}` | Détail d'un sous-programme |
| `SubProgramController` | POST | `/api/subprograms/{id}/calculate-route` | Déclenche calcul Valhalla |
| `OptimizationController` | POST | `/api/optimization/programs/{id}` | Lance l'optimisation |
| `OptimizationController` | GET | `/api/optimization/programs/{id}/stats` | Statistiques d'optimisation |

---

## Couche Config (`config/`)

| Classe | Rôle |
|---|---|
| `WebClientConfig` | Bean `WebClient.Builder` pour les appels HTTP |
| `SecurityConfig` | Sécurité JWT personnalisée (BCrypt), CORS, Swagger & Auth ouverts |
| `PasswordEncoderConfig` | Configuration du hachage de mot de passe BCrypt |
| `JwtAuthenticationFilter` | Filtre stateless de sécurité JWT |
| `WebSocketConfig` | STOMP sur `/ws`, topics sur `/topic/*`, prefixe app `/app` |

---

## Base de données — Supabase (cloud)

Supabase gère PostgreSQL 15 + PostGIS + backups + SSL.

### Fichier `.env` (à remplir)
```env
DB_URL=jdbc:postgresql://aws-0-{region}.pooler.supabase.com:6543/postgres
DB_USERNAME=postgres.YOUR_PROJECT_REF
DB_PASSWORD=YOUR_SUPABASE_DB_PASSWORD
VALHALLA_URL=http://localhost:8002
JWT_SECRET=votre_secret_jwt_souverain_ici
```

---

## Valhalla Local

Valhalla est le moteur de calcul d'itinéraires et de tracés cartographiques.
Lancé localement via Docker sur le port `8002` avec l'extrait géographique du Maroc (`morocco-latest.osm.pbf`). 

Test de santé opérationnel :
```bash
curl http://localhost:8002/status
```

---

## Conventions

- Timestamps : `LocalDateTime` en UTC
- PostGIS : `Point(longitude, latitude)` avec SRID 4326
- Erreurs : JSON `{ timestamp, status, error, message }` via `GlobalExceptionHandler`
- Sécurité : JWT local stateless via SecurityContextHolder

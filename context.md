# SmartFleet Backend — Context

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
| Sécurité | Spring Security + OAuth2 Resource Server (Clerk JWT) |
| HTTP Client | Spring WebFlux `WebClient` |
| WebSocket | Spring WebSocket + STOMP |
| Notifications | Firebase Admin SDK (FCM — stub prêt à câbler) |
| Optimisation VRP | Google OR-Tools (round-robin en attendant) |
| Routage | Valhalla (à setup localement via Docker) |
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
│   └── NotificationService.java
├── controller/
│   ├── HealthController.java
│   ├── OrderController.java
│   ├── DriverController.java
│   ├── SubProgramController.java
│   └── OptimizationController.java
├── config/
│   ├── WebClientConfig.java
│   ├── SecurityConfig.java
│   └── WebSocketConfig.java
├── exception/
│   ├── SmartFleetException.java
│   ├── ResourceNotFoundException.java
│   ├── ValhallaServiceException.java
│   ├── OptimizationException.java
│   └── GlobalExceptionHandler.java
└── dto/
    ├── UserDTO.java
    ├── OrderDTO.java
    ├── SubProgramDTO.java
    ├── DeliveryProgramDTO.java
    └── DriverLocationUpdateDTO.java
```

---

## Couche Model (`model/`)

| Entité | Description |
|---|---|
| `User` | Base avec héritage JOINED → `Manager`, `Driver`, `Client` |
| `Manager` | Responsable d'une équipe de chauffeurs et véhicules |
| `Driver` | Chauffeur avec position GPS (PostGIS `Point`) |
| `Client` | Client final avec ses commandes |
| `Vehicle` | Véhicule avec `maxPayloadKg` et `maxVolumeM2` |
| `Order` | Commande avec coordonnées GPS et PostGIS `Point` |
| `SubProgram` | Tournée : 1 chauffeur + 1 véhicule + N commandes |
| `DeliveryProgram` | Programme de livraison groupant les sous-programmes |

---

## Couche Repository (`repository/`)

8 repositories Spring Data JPA. Requêtes notables :
- `DriverRepository.findByManagerIdAndAvailableTrue()` — chauffeurs dispo
- `DriverRepository.findDriversNearby()` — requête PostGIS ST_DWithin
- `OrderRepository.findOrdersNearby()` — commandes proches d'un point GPS
- `DeliveryProgramRepository.findByManagerIdAndStatus()` — filtrage par statut

---

## Couche Service (`service/`)

| Service | Responsabilité |
|---|---|
| `ValhallaClient` | Client HTTP pour Valhalla (`/route`, `/optimized_route`, `/matrix`) |
| `RouteService` | Calcule l'itinéraire optimal d'un `SubProgram` via Valhalla |
| `OrderService` | Approbation/rejet de livraisons, complétion automatique des sous-programmes |
| `LocationTrackingService` | Mise à jour GPS, PostGIS, Haversine distance |
| `DeliveryOptimizationService` | Crée les `SubProgram` par assignation round-robin (OR-Tools VRP prévu) |
| `NotificationService` | Push FCM vers drivers et clients (log stub, prêt à câbler) |

---

## Couche Controller (`controller/`)

Base path : `/api` (via `server.servlet.context-path=/api`)

| Controller | Méthode | Endpoint | Description |
|---|---|---|---|
| `HealthController` | GET | `/api/health` | Statut de l'application |
| `OrderController` | GET | `/api/orders/{id}` | Détail d'une commande |
| `OrderController` | POST | `/api/orders/{id}/approve` | Client approuve la livraison |
| `OrderController` | POST | `/api/orders/{id}/reject` | Rejeter une commande |
| `DriverController` | PUT | `/api/drivers/{id}/location` | Mise à jour GPS du chauffeur |
| `DriverController` | GET | `/api/drivers/{id}/nearby` | Vérifie proximité d'un point |
| `SubProgramController` | GET | `/api/subprograms/{id}` | Détail d'un sous-programme |
| `SubProgramController` | POST | `/api/subprograms/{id}/calculate-route` | Déclenche calcul Valhalla |
| `OptimizationController` | POST | `/api/optimization/programs/{id}` | Lance l'optimisation |
| `OptimizationController` | GET | `/api/optimization/programs/{id}/stats` | Statistiques d'optimisation |

---

## Couche Config (`config/`)

| Classe | Rôle |
|---|---|
| `WebClientConfig` | Bean `WebClient.Builder` pour les appels HTTP |
| `SecurityConfig` | Stateless JWT via JWKS Clerk, CORS, accès public `/api/health` |
| `WebSocketConfig` | STOMP sur `/ws`, topics sur `/topic/*`, prefixe app `/app` |

---

## Couche Exception (`exception/`)

| Classe | HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `OptimizationException` | 422 |
| `ValhallaServiceException` | 503 |
| `IllegalArgumentException` | 400 |
| `IllegalStateException` | 409 |
| `MethodArgumentNotValidException` | 400 |
| `Exception` (fallback) | 500 |

Tous retournent un JSON structuré `{ timestamp, status, error, message }`.

---

## Base de données — Supabase (cloud)

> **Aucune installation locale requise.** Supabase gère PostgreSQL 15 + PostGIS + backups + SSL.

### Setup (une seule fois)
1. Créer un projet sur [supabase.com](https://supabase.com)
2. Activer PostGIS : `CREATE EXTENSION IF NOT EXISTS postgis;`
3. Copier les credentials dans `.env`

### Fichier `.env` (à remplir)
```env
DB_URL=jdbc:postgresql://aws-0-{region}.pooler.supabase.com:6543/postgres
DB_USERNAME=postgres.YOUR_PROJECT_REF
DB_PASSWORD=YOUR_SUPABASE_DB_PASSWORD
CLERK_ISSUER=https://YOUR_CLERK_DOMAIN
CLERK_AUDIENCE=https://YOUR_CLERK_DOMAIN
VALHALLA_URL=http://localhost:8002
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
```

**Les tables sont créées automatiquement** au premier démarrage (`ddl-auto=update`).

---

## Valhalla (en attente de setup)

Valhalla est le moteur de calcul d'itinéraires. Il n'est **pas encore configuré**.  
En attendant, `DeliveryOptimizationService` utilise un **algorithme round-robin** pour assigner les commandes.

Pour le démarrer localement :
```bash
docker-compose up -d valhalla
```

Une fois lancé, `RouteService.calculateOptimalRoute()` et `ValhallaClient` fonctionneront automatiquement.

---

## Prochaines étapes

- [ ] Configurer Valhalla (Docker local)
- [ ] Remplacer round-robin par OR-Tools VRP dans `DeliveryOptimizationService`
- [ ] Câbler Firebase FCM dans `NotificationService`
- [ ] Ajouter `DeliveryProgramController`, `VehicleController`, `UserController`
- [ ] Écrire les tests unitaires (`OrderService`, `LocationTrackingService`)
- [ ] Passer `ddl-auto` de `update` à `validate` après stabilisation du schéma

---

## Conventions

- Timestamps : `LocalDateTime` en UTC
- PostGIS : `Point(longitude, latitude)` avec SRID 4326
- Erreurs : JSON `{ timestamp, status, error, message }` via `GlobalExceptionHandler`
- Sécurité : JWT Clerk validé via JWKS — stateless, pas de session

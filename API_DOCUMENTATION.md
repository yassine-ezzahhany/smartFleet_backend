# 📘 SmartFleet - Documentation des APIs

Cette documentation regroupe l'ensemble des points d'accès (endpoints) de l'API de **SmartFleet Backend** classés par famille fonctionnelle, avec leurs spécifications techniques (méthode, chemin, paramètres, corps de requête, autorisations, et exemples de payloads).

---

## 🔒 Configuration Globale de Sécurité & Accès
* **Préfixe global de l'API** : `/api`
* **Sécurité** : L'accès à la majorité des endpoints nécessite un jeton JWT valide transmis sous forme de header HTTP : `Authorization: Bearer <token_jwt>`.
* **Rôles (RBAC)** : `ADMIN`, `MANAGER`, `DRIVER`, `CLIENT`.

---

## 1. Famille : Authentification & Profil (`/api/auth`)
Permet de gérer l'inscription, la connexion et l'accès au profil de l'utilisateur connecté de manière locale et stateless.

### 📥 Inscription d'un Utilisateur
* **Méthode** : `POST`
* **Chemin** : `/api/auth/register`
* **Accès** : Public
* **Corps de la requête (`RegisterRequestDTO`)** :
  ```json
  {
    "email": "manager@smartfleet.ma",
    "password": "MotDePasseSecurise123",
    "name": "Yassine Ezzahhany",
    "phone": "+212600000000",
    "role": "MANAGER",
    "companyName": "SmartFleet Corp",        // Optionnel (Requis pour CLIENT)
    "businessAddress": "FST Settat, Maroc",   // Optionnel (Requis pour CLIENT)
    "businessPhone": "+212523000000",         // Optionnel (Requis pour CLIENT)
    "department": "Logistique"                // Optionnel (Requis pour MANAGER)
  }
  ```
* **Réponse** (`201 CREATED` ou `400 BAD REQUEST`) :
  ```json
  {
    "id": 1,
    "email": "manager@smartfleet.ma",
    "name": "Yassine Ezzahhany",
    "phone": "+212600000000",
    "role": "MANAGER",
    "active": true
  }
  ```

---

### 🔑 Connexion d'un Utilisateur
* **Méthode** : `POST`
* **Chemin** : `/api/auth/login`
* **Accès** : Public
* **Corps de la requête (`LoginRequestDTO`)** :
  ```json
  {
    "email": "manager@smartfleet.ma",
    "password": "MotDePasseSecurise123"
  }
  ```
* **Réponse** (`200 OK` ou `401 UNAUTHORIZED`) :
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYW5hZ2VyQHNtYXJ0ZmxlZXQubWEiLCJpZCI6MSwicm9sZSI6Ik1BTkFHRVIifQ...",
    "user": {
      "id": 1,
      "email": "manager@smartfleet.ma",
      "name": "Yassine Ezzahhany",
      "phone": "+212600000000",
      "role": "MANAGER",
      "active": true
    }
  }
  ```

---

### 👤 Profil Courant Connecté
* **Méthode** : `GET`
* **Chemin** : `/api/auth/me`
* **Accès** : Authentifié (Tous les rôles)
* **Réponse** (`200 OK` ou `401 UNAUTHORIZED`) :
  ```json
  {
    "id": 1,
    "email": "manager@smartfleet.ma",
    "name": "Yassine Ezzahhany",
    "phone": "+212600000000",
    "role": "MANAGER",
    "active": true
  }
  ```

---

### ✏️ Mise à jour du Profil
* **Méthode** : `PUT`
* **Chemin** : `/api/auth/me`
* **Accès** : Authentifié (Tous les rôles)
* **Corps de la requête (`UserDTO`)** :
  ```json
  {
    "name": "Yassine Ezzahhany Modifié",
    "phone": "+212611223344"
  }
  ```
* **Réponse** (`200 OK`) :
  ```json
  {
    "id": 1,
    "email": "manager@smartfleet.ma",
    "name": "Yassine Ezzahhany Modifié",
    "phone": "+212611223344",
    "role": "MANAGER",
    "active": true
  }
  ```

---

## 2. Famille : Commandes (`/api/orders`)
Gestion du cycle de vie des commandes à livrer (création, consultation, approbation et rejet).

### 📦 Créer une Commande
* **Méthode** : `POST`
* **Chemin** : `/api/orders`
* **Accès** : Authentifié (`MANAGER`)
* **Corps de la requête (`OrderDTO`)** :
  ```json
  {
    "orderNumber": "ORD-2026-001",
    "clientId": 3,
    "weightKg": 150.0,
    "volumeM2": 1.2,
    "deliveryLatitude": 33.5731,
    "deliveryLongitude": -7.5898,
    "deliveryAddress": "Boulevard Al Massira Al Khadra, Maarif, Casablanca",
    "deliveryDescription": "Livrer avant 12h - Entrepôt Sapino"
  }
  ```
* **Réponse** (`201 CREATED`) :
  ```json
  {
    "id": 12,
    "orderNumber": "ORD-2026-001",
    "clientId": 3,
    "weightKg": 150.0,
    "volumeM2": 1.2,
    "deliveryLatitude": 33.5731,
    "deliveryLongitude": -7.5898,
    "deliveryAddress": "Boulevard Al Massira Al Khadra, Maarif, Casablanca",
    "deliveryDescription": "Livrer avant 12h - Entrepôt Sapino",
    "status": "PENDING",
    "clientApproved": false,
    "estimatedDeliveryTime": null,
    "actualDeliveryTime": null
  }
  ```

---

### 📋 Lister toutes les Commandes
* **Méthode** : `GET`
* **Chemin** : `/api/orders`
* **Accès** : Authentifié (`MANAGER`, `DRIVER`)
* **Réponse** (`200 OK`) :
  ```json
  [
    {
      "id": 12,
      "orderNumber": "ORD-2026-001",
      "clientId": 3,
      "status": "PENDING",
      "weightKg": 150.0,
      "volumeM2": 1.2,
      "deliveryLatitude": 33.5731,
      "deliveryLongitude": -7.5898,
      "deliveryAddress": "Boulevard Al Massira Al Khadra, Maarif, Casablanca",
      "clientApproved": false
    }
  ]
  ```

---

### 🔍 Consulter une Commande Spécifique
* **Méthode** : `GET`
* **Chemin** : `/api/orders/{id}`
* **Accès** : Authentifié (Tous les rôles)
* **Réponse** (`200 OK` ou `404 NOT FOUND`)

---

### 🟢 Approuver une Livraison
Permet au client (ou manager) de valider la bonne réception de la commande. Déclenche la vérification de fin de tournée pour le sous-programme.
* **Méthode** : `POST`
* **Chemin** : `/api/orders/{id}/approve`
* **Accès** : Authentifié (`CLIENT`, `MANAGER`)
* **Réponse** (`200 OK`) :
  ```json
  {
    "id": 12,
    "status": "DELIVERED",
    "clientApproved": true,
    "actualDeliveryTime": "2026-06-15T17:35:00"
  }
  ```

---

### 🛑 Rejeter une Livraison
Permet d'annuler ou refuser une commande avec motif.
* **Méthode** : `POST`
* **Chemin** : `/api/orders/{id}/reject`
* **Accès** : Authentifié (`CLIENT`, `MANAGER`, `DRIVER`)
* **Corps de la requête** : Chaîne brute contenant la raison du rejet.
  ```text
  Client absent lors du passage du livreur.
  ```
* **Réponse** (`200 OK`) :
  ```json
  {
    "id": 12,
    "status": "REJECTED",
    "clientApproved": false
  }
  ```

---

### ✏️ Mettre à jour le Statut
* **Méthode** : `PUT`
* **Chemin** : `/api/orders/{id}/status`
* **Accès** : Authentifié (`DRIVER`, `MANAGER`)
* **Paramètres de requête** :
  * `status` (String, obligatoire) : Ex. `IN_TRANSIT`, `ARRIVING_SOON`, `DELIVERED`, `REJECTED`, `CANCELLED`
* **Réponse** (`200 OK`)

---

## 3. Famille : Véhicules (`/api/vehicles`)
Gestion de la flotte automobile du manager connecté (poids et volume utiles).

### 🚚 Ajouter un Véhicule
* **Méthode** : `POST`
* **Chemin** : `/api/vehicles`
* **Accès** : Authentifié (`MANAGER`)
* **Corps de la requête (`VehicleDTO`)** :
  ```json
  {
    "vehicleNumber": "VEH-001",
    "model": "Renault Kangoo",
    "licensePlate": "12345-A-15",
    "maxWeightKg": 800.0,
    "maxVolumeM3": 3.0,
    "active": true
  }
  ```
* **Réponse** (`201 CREATED`)

---

### 📋 Lister mes Véhicules
* **Méthode** : `GET`
* **Chemin** : `/api/vehicles`
* **Accès** : Authentifié (`MANAGER`)
* **Réponse** (`200 OK`) :
  ```json
  [
    {
      "id": 5,
      "vehicleNumber": "VEH-001",
      "model": "Renault Kangoo",
      "maxWeightKg": 800.0,
      "maxVolumeM3": 3.0,
      "active": true
    }
  ]
  ```

---

### 🟢 Lister mes Véhicules Actifs
* **Méthode** : `GET`
* **Chemin** : `/api/vehicles/active`
* **Accès** : Authentifié (`MANAGER`)

---

### ✏️ Mettre à jour un Véhicule
* **Méthode** : `PUT`
* **Chemin** : `/api/vehicles/{id}`
* **Accès** : Authentifié (`MANAGER` propriétaire)

---

### ⚙️ Activer/Désactiver un Véhicule
* **Méthode** : `PATCH`
* **Chemin** : `/api/vehicles/{id}/toggle`
* **Accès** : Authentifié (`MANAGER` propriétaire)
* **Paramètres de requête** :
  * `active` (boolean, obligatoire) : `true` pour activer, `false` pour désactiver.
* **Réponse** (`204 NO CONTENT`)

---

## 4. Famille : Conducteurs (`/api/drivers`)
Gestion des chauffeurs de la flotte, affectation et suivi GPS en temps réel.

### 📍 Mettre à jour la Position GPS (Mobile)
Envoyé par l'application mobile toutes les 30 secondes en tâche de fond pour mettre à jour la position PostGIS.
* **Méthode** : `PUT`
* **Chemin** : `/api/drivers/{id}/location`
* **Accès** : Authentifié (`DRIVER`)
* **Corps de la requête (`DriverLocationUpdateDTO`)** :
  ```json
  {
    "driverId": 4,
    "latitude": 33.9716,
    "longitude": -6.8498,
    "timestamp": 1773665400000
  }
  ```
* **Réponse** (`200 OK`)

---

### 🔍 Vérifier la Proximité du Point
Permet de vérifier si le chauffeur est à portée géospatiale d'un point de livraison (en kilomètres).
* **Méthode** : `GET`
* **Chemin** : `/api/drivers/{id}/nearby`
* **Accès** : Authentifié
* **Paramètres de requête** :
  * `lat` (Double, obligatoire) : Latitude cible
  * `lon` (Double, obligatoire) : Longitude cible
  * `radiusKm` (Double, optionnel, défaut = 1.0) : Rayon de recherche en km
* **Réponse** (`200 OK`) :
  ```json
  {
    "nearby": true
  }
  ```

---

### 👥 Lister les Chauffeurs sans Manager
Permet au manager de recruter de nouveaux conducteurs dans sa flotte.
* **Méthode** : `GET`
* **Chemin** : `/api/drivers/unassigned`
* **Accès** : Authentifié (`MANAGER`)

---

### 📋 Lister mes Chauffeurs
* **Méthode** : `GET`
* **Chemin** : `/api/drivers/my-drivers`
* **Accès** : Authentifié (`MANAGER`)

---

### 🗺️ Récupérer les Positions GPS en Temps Réel
Permet d'afficher la carte globale de dispatching avec la position de chaque chauffeur actif.
* **Méthode** : `GET`
* **Chemin** : `/api/drivers/my-drivers/locations`
* **Accès** : Authentifié (`MANAGER`)
* **Réponse** (`200 OK`) :
  ```json
  [
    {
      "id": 4,
      "email": "driver@smartfleet.ma",
      "name": "Omar Laroui",
      "phone": "+212699887766",
      "active": true,
      "licenseNumber": "15/998877",
      "licenseExpiry": "2030-12-31",
      "available": true,
      "managerId": 1,
      "currentLatitude": 33.9716,
      "currentLongitude": -6.8498,
      "lastLocationUpdate": 1773665400000
    }
  ]
  ```

---

### 🤝 Affecter un Chauffeur à ma Flotte
* **Méthode** : `POST`
* **Chemin** : `/api/drivers/{id}/assign`
* **Accès** : Authentifié (`MANAGER`)

---

### 💔 Retirer un Chauffeur de ma Flotte
* **Méthode** : `POST`
* **Chemin** : `/api/drivers/{id}/unassign`
* **Accès** : Authentifié (`MANAGER`)

---

## 5. Famille : Programmes de Livraison (`/api/programs`)
Création et structuration des lots quotidiens de commandes avant calcul d'optimisation.

### 📁 Créer un Programme
* **Méthode** : `POST`
* **Chemin** : `/api/programs`
* **Accès** : Authentifié (`MANAGER`)
* **Corps de la requête (`DeliveryProgramDTO`)** :
  ```json
  {
    "programNumber": "PROG-2026-06-15",
    "description": "Livraisons Casablanca-Settat du 15 Juin"
  }
  ```
* **Réponse** (`201 CREATED`)

---

### 📋 Lister mes Programmes
* **Méthode** : `GET`
* **Chemin** : `/api/programs`
* **Accès** : Authentifié (`MANAGER`)
* **Paramètres de requête** :
  * `status` (String, optionnel) : Filtre par statut (Ex: `PENDING`, `OPTIMIZED`, `IN_PROGRESS`)

---

### 🔍 Consulter un Programme
* **Méthode** : `GET`
* **Chemin** : `/api/programs/{id}`
* **Accès** : Authentifié (`MANAGER` propriétaire)

---

### 🔗 Associer des Commandes à un Programme
* **Méthode** : `POST`
* **Chemin** : `/api/programs/{id}/orders`
* **Accès** : Authentifié (`MANAGER` propriétaire)
* **Corps de la requête** : `List<Long>` (Liste d'identifiants de commandes)
  ```json
  [12, 13, 14]
  ```
* **Réponse** (`200 OK`)

---

### ❌ Retirer une Commande d'un Programme
* **Méthode** : `DELETE`
* **Chemin** : `/api/programs/{id}/orders/{orderId}`
* **Accès** : Authentifié (`MANAGER` propriétaire)

---

## 6. Famille : Optimisation (`/api/optimization`)
Lancement des algorithmes de recherche opérationnelle pour le regroupement et ordonnancement.

### 🤖 Lancer l'Optimisation de Tournées (VRP)
Prend les commandes d'un programme, les véhicules et chauffeurs actifs, résout le Vehicle Routing Problem sous contraintes avec Google OR-Tools, puis génère automatiquement les sous-programmes (tournées).
* **Méthode** : `POST`
* **Chemin** : `/api/optimization/programs/{id}`
* **Accès** : Authentifié (`MANAGER` propriétaire)
* **Réponse** (`200 OK`) : Renvoie le `DeliveryProgramDTO` mis à jour au statut `OPTIMIZED` contenant la liste des sous-programmes créés.

---

### 📊 Statistiques d'Optimisation
Fournit les métriques clés de l'exécution de l'algorithme (nombre de routes, taux d'occupation, distance totale, temps économisé).
* **Méthode** : `GET`
* **Chemin** : `/api/optimization/programs/{id}/stats`
* **Accès** : Authentifié (`MANAGER`)
* **Réponse** (`200 OK`) :
  ```json
  {
    "totalDistanceKm": 240.5,
    "totalDurationMinutes": 320,
    "vehicleUsagePercentage": 80.0,
    "optimizedRoutesCount": 3,
    "unassignedOrdersCount": 0
  }
  ```

---

## 7. Famille : Sous-Programmes / Tournées (`/api/subprograms`)
Gestion opérationnelle d'une tournée assignée à un chauffeur et un véhicule spécifique.

### 🗺️ Calculer l'Itinéraire Géométrique (Valhalla)
Appelle le serveur de routage local Valhalla pour tracer le tracé de route optimal (polyline) reliant les arrêts de livraison ordonnés, et calcule les métriques de distance et durée prévisionnelles.
* **Méthode** : `POST`
* **Chemin** : `/api/subprograms/{id}/calculate-route`
* **Accès** : Authentifié
* **Réponse** (`200 OK`) :
  ```json
  {
    "id": 8,
    "subProgramNumber": "SUB-PROG-08",
    "polyline": "g_zeF~e_dEv@aHkC{IaF...",
    "estimatedDistanceKm": 45.2,
    "estimatedDurationMinutes": 62,
    "status": "ASSIGNED",
    "orderIds": [12, 13]
  }
  ```

---

### 🚀 Démarrer la Tournée (Chauffeur)
Passe le sous-programme et toutes ses commandes rattachées au statut `IN_TRANSIT`. Débute l'horodatage.
* **Méthode** : `PUT`
* **Chemin** : `/api/subprograms/{id}/start`
* **Accès** : Authentifié (`DRIVER` assigné ou `MANAGER`)
* **Réponse** (`200 OK`)

---

### 📍 Récupérer ma Tournée Active (Mobile)
* **Méthode** : `GET`
* **Chemin** : `/api/subprograms/my-active`
* **Accès** : Authentifié (`DRIVER`)

---

### 📋 Récupérer l'Historique de mes Tournées
* **Méthode** : `GET`
* **Chemin** : `/api/subprograms/my-subprograms`
* **Accès** : Authentifié (`DRIVER`)

---

## 8. Famille : Statut Système (`/api/health`)
Utilitaires de surveillance du serveur.

### 🩺 Health Check
* **Méthode** : `GET`
* **Chemin** : `/api/health`
* **Accès** : Public
* **Réponse** (`200 OK`) :
  ```json
  {
    "status": "UP",
    "service": "SmartFleet Backend",
    "timestamp": "2026-06-15T17:42:00.123"
  }
  ```

---

## 📡 9. Flux WebSocket STOMP (Temps Réel)
Le serveur intègre un Message Broker STOMP pour la diffusion réactive des positions GPS sans surcharge de base de données.

* **URL de Connexion Handshake** : `ws://localhost:8080/api/ws` (avec fallback SockJS)
* **Préfixe d'envoi Client** : `/app`
* **Préfixe d'écoute Broker** : `/topic`

### 📢 Topics d'écoute (Subscribers)
1. **Canal Global Flotte Manager** : `/topic/managers/{managerId}/drivers`
   * **Utilité** : Le dashboard Manager écoute ce topic pour mettre à jour la position de tous ses camions sur la carte.
   * **Payload reçu** :
     ```json
     {
       "driverId": 4,
       "name": "Omar Laroui",
       "latitude": 33.9716,
       "longitude": -6.8498,
       "timestamp": 1773665400000
     }
     ```
2. **Canal Client Individuel** : `/topic/drivers/{driverId}/location`
   * **Utilité** : Le client en attente de livraison écoute ce canal pour voir avancer le chauffeur assigné à sa commande.
   * **Payload reçu** :
     ```json
     {
       "driverId": 4,
       "latitude": 33.9716,
       "longitude": -6.8498,
       "timestamp": 1773665400000
     }
     ```

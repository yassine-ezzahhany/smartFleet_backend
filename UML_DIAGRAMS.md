# 📊 SmartFleet - Analyse et Conception UML

Ce document présente l'analyse et la conception UML du système de gestion logistique **SmartFleet**, générées sous forme d'images et basées sur l'architecture et le code source réels du backend.

---

## 1. Diagramme de Classes (Class Diagram)
Ce diagramme représente le modèle de données persistant (JPA) et les relations clés d'héritage, d'association et de composition entre les entités du système.

![Diagramme de classes UML représentant la structure JPA et les relations de données de SmartFleet](C:/Users/Lenovo/.gemini/antigravity-ide/brain/2a1edee6-19b7-41f2-96bf-d86ea32262e1/class_diagram_uml_1781543622256.png)

### 💡 Description de la Modélisation BDD / JPA :
* **Héritage d'Utilisateurs (`User`)** : La classe de base `User` utilise la stratégie JPA `JOINED` pour se décliner en trois classes de rôles spécifiques : `Manager`, `Driver` (Conducteur) et `Client`.
* **Gestion de la Flotte** : Le `Manager` possède une relation `OneToMany` avec `Vehicle`.
* **Affectation Chauffeurs & Véhicules** : Une relation `ManyToMany` associe les chauffeurs (`Driver`) à leurs véhicules assignés (`Vehicle`).
* **Optimisation des Tournées** : Un programme de livraison (`DeliveryProgram`) créé par un `Manager` contient plusieurs commandes (`Order`) et se divise en plusieurs sous-programmes (`SubProgram`), chacun représentant une route/tournée physique assignée à un `Driver` et un `Vehicle`.

---

## 2. Diagramme de Cas d'Usage (Use Case Diagram)
Ce diagramme détaille les fonctionnalités du système du point de vue des acteurs (`Manager`, `Driver`, `Client`).

![Diagramme de cas d'usage UML modélisant les fonctionnalités de SmartFleet](C:/Users/Lenovo/.gemini/antigravity-ide/brain/2a1edee6-19b7-41f2-96bf-d86ea32262e1/use_case_diagram_uml_1781543641559.png)

### 💡 Rôles et Actions associés dans l'API :
* **Manager (Gestionnaire)** :
  * Importer les commandes et gérer la flotte (`/api/orders`, `/api/vehicles`).
  * Assigner les chauffeurs et gérer les programmes (`/api/drivers/{id}/assign`, `/api/programs`).
  * Lancer l'optimisation des tournées (`/api/optimization/programs/{id}`).
* **Driver (Chauffeur)** :
  * Démarrer une tournée et consulter son itinéraire (`/api/subprograms/{id}/start`).
  * Envoyer ses coordonnées géographiques en tâche de fond (`/api/drivers/{id}/location`).
* **Client (Destinataire)** :
  * Suivre l'acheminement de son colis sur la carte en temps réel (via le topic WebSocket `/topic/drivers/{driverId}/location`).
  * Approuver ou rejeter la réception du colis (`/api/orders/{id}/approve`, `/api/orders/{id}/reject`).

---

## 3. Diagramme d'Activités (Activity Diagram)
Ce diagramme modélise le processus global de planification, d'optimisation (VRP), de calcul d'itinéraire routier (Valhalla) et de suivi télématique en temps réel.

![Diagramme d'activités UML du flux complet de gestion de tournée de SmartFleet](C:/Users/Lenovo/.gemini/antigravity-ide/brain/2a1edee6-19b7-41f2-96bf-d86ea32262e1/activity_diagram_uml_1781543657991.png)

### 💡 Logique du Flux Métier :
1. **Planification** : Le manager importe les commandes et crée un programme de livraison au statut `PENDING`.
2. **Recherche Opérationnelle** : L'algorithme résout le problème de tournées de véhicules (VRP) sous contraintes de charge avec Google OR-Tools. Les commandes sont regroupées et assignées à des sous-programmes au statut `PENDING`.
3. **Calcul d'Itinéraire** : Le serveur de routage local Valhalla calcule le tracé précis de la route et les métriques de distance et durée. Le sous-programme passe au statut `ASSIGNED`.
4. **Transit et Télématique** : Le chauffeur démarre sa tournée (`IN_TRANSIT`). L'application mobile transmet les coordonnées GPS par intervalle de 30 secondes via le canal WebSocket STOMP.
5. **Livraison & Clôture** : Le client valide la réception de sa commande. Une fois que toutes les commandes du sous-programme sont approuvées, la tournée se clôture automatiquement au statut `COMPLETED`.

---

## 4. Diagrammes de Séquence (Sequence Diagrams)

Ces diagrammes représentent les interactions temporelles et les échanges de messages entre les différents objets du système lors de deux cas d'usage critiques.

### A. Flux d'Optimisation des Tournées (VRP)
Ce diagramme détaille la séquence des événements lors du lancement de l'algorithme d'optimisation par le Manager pour un programme de livraison donné.

![Diagramme de séquence UML du processus d'optimisation de tournée de SmartFleet](C:/Users/Lenovo/.gemini/antigravity-ide/brain/2a1edee6-19b7-41f2-96bf-d86ea32262e1/sequence_diagram_optimization_uml_1781544100410.png)

#### 💡 Explications du flux :
1. Le **Manager** lance l'optimisation via le contrôleur REST `OptimizationController`.
2. Le contrôleur appelle `DeliveryOptimizationService` qui charge les commandes et les ressources (véhicules et chauffeurs actifs) depuis la base de données.
3. Le service délègue la résolution mathématique à `ORToolsAdapter` qui configure le solveur VRP sous contraintes de charge (poids, volume).
4. Le solveur OR-Tools génère les groupes optimaux. Le service crée alors les entités `SubProgram` (les tournées).
5. Le programme mis à jour est sauvegardé via `DeliveryProgramService` dans la base de données PostgreSQL, puis le statut mis à jour est retourné à l'utilisateur.

---

### B. Suivi GPS et Télématique Temps Réel
Ce diagramme illustre le flux réactif d'envoi et de diffusion continue de la géolocalisation des chauffeurs en transit.

![Diagramme de séquence UML du suivi GPS temps réel de SmartFleet](C:/Users/Lenovo/.gemini/antigravity-ide/brain/2a1edee6-19b7-41f2-96bf-d86ea32262e1/sequence_diagram_location_tracking_uml_1781544117666.png)

#### 💡 Explications du flux :
1. L'application mobile du **Driver** transmet les coordonnées GPS actuelles au serveur via l'endpoint `/api/drivers/{id}/location`.
2. Le contrôleur REST appelle `LocationTrackingService`.
3. Le service met à jour la position géographique du chauffeur dans la base de données à l'aide de l'extension spatiale PostGIS (création d'un objet géométrique `Point` avec le SRID 4326).
4. Pour éviter les requêtes HTTP répétitives et les surcharges de base de données, la position est immédiatement poussée vers le Message Broker STOMP via `SimpMessagingTemplate`.
5. Le broker WebSocket diffuse la nouvelle position à tous les abonnés concernés : le Manager qui supervise la flotte globale, et le Client qui suit en direct l'approche de son colis.

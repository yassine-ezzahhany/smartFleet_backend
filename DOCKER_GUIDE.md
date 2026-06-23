# 🐳 Guide de Démarrage Rapide avec Docker — SmartFleet Backend

Ce projet a été entièrement dockerisé pour faciliter son déploiement et son test par votre professeur ou tout autre correcteur. Grâce à cette configuration, il n'est pas nécessaire d'installer Java, Maven, PostgreSQL, PostGIS ou Valhalla localement sur la machine hôte. Tout est orchestré de manière autonome.

---

## 📋 Prérequis

Avoir **Docker** et **Docker Compose** installés sur votre système :
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows / macOS)
- Docker Engine & Docker Compose (Linux)

---

## 🚀 Lancement Rapide (Une seule commande)

Ouvrez un terminal dans le dossier racine du projet (contenant le fichier `docker-compose.yml`) et exécutez la commande suivante :

```bash
docker compose up --build -d
```

### Que fait cette commande ?
1. **db** : Démarre un conteneur avec PostgreSQL 16 et PostGIS 3.4. La base de données `smartfleet_db_dev` et l'extension spatiale `postgis` y sont configurées automatiquement.
2. **valhalla** : Démarre le moteur de calcul d'itinéraires Valhalla. Au premier lancement, il télécharge les données routières du Maroc (~200MB Geofabrik) et compile les tuiles de navigation.
3. **backend** : Compile le code source Java 17 via un conteneur Maven temporaire (multi-stage build) puis démarre le serveur d'application Spring Boot connecté à la BDD locale et à Valhalla.

> [!IMPORTANT]
> **Premier Démarrage (Valhalla) :**
> Le premier lancement peut prendre de **2 à 5 minutes** car Valhalla doit télécharger et compiler les données géographiques du Maroc. Les lancements ultérieurs seront **instantanés** grâce au volume de cache Docker.

---

## 🔍 Vérification du Démarrage

Pour suivre l'état d'avancement du démarrage et voir si tout est fonctionnel :

1. **Vérifier le statut des services :**
   ```bash
   docker compose ps
   ```
   *Tous les services (`smartfleet-db`, `smartfleet-valhalla`, `smartfleet-backend`) doivent être indiqués comme `running` (en cours d'exécution) et sains.*

2. **Consulter les logs du Backend en temps réel :**
   ```bash
   docker compose logs -f backend
   ```
   *Vous devez voir la bannière Spring Boot s'afficher et le message de succès de démarrage sur le port 8080.*

3. **Tester le statut de l'API (Health Check) :**
   Accédez à [http://localhost:8080/api/health](http://localhost:8080/api/health) dans votre navigateur.  
   Vous devriez recevoir cette réponse JSON indiquant que tout est prêt :
   ```json
   {
     "status": "UP",
     "service": "SmartFleet Backend",
     "timestamp": "2026-06-23T16:20:00.123456"
   }
   ```

---

## 🔌 Accès aux Services et documentations

Une fois le projet démarré, les interfaces suivantes sont disponibles :

*   **Documentation interactive de l'API (Swagger UI) :** [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html) (très pratique pour tester chaque endpoint directement).
*   **Documentation OpenAPI au format JSON :** [http://localhost:8080/api/v3/api-docs](http://localhost:8080/api/v3/api-docs)
*   **Base de Données PostgreSQL + PostGIS :**
    *   **Hôte :** `localhost` (depuis votre machine) ou `db` (depuis un conteneur Docker)
    *   **Port :** `5432`
    *   **Utilisateur :** `smartfleet_user`
    *   **Mot de passe :** `smartfleet_password`
    *   **Base de données :** `smartfleet_db_dev`
*   **Service Valhalla (API Routage) :** [http://localhost:8002/health](http://localhost:8002/health)

---

## 🛠️ Commandes Utiles de Maintenance

*   **Arrêter les services (sans perdre les données de la base) :**
    ```bash
    docker compose down
    ```

*   **Arrêter les services ET supprimer les données de la base (réinitialisation complète) :**
    ```bash
    docker compose down -v
    ```

*   **Consulter les logs de tous les conteneurs :**
    ```bash
    docker compose logs -f
    ```

*   **Forcer la recompilation du Backend (après modification du code source) :**
    ```bash
    docker compose up --build -d backend
    ```

---

## 🆘 Résolution des Problèmes (Troubleshooting)

### 1. Port 8080 ou 5432 déjà occupé
Si vous avez déjà un serveur web (ex: Tomcat, Node) ou une base de données PostgreSQL qui tourne sur votre machine hôte, docker-compose affichera une erreur de type `port is already allocated`.
*   **Solution :** Arrêtez vos services locaux ou modifiez les ports exposés à gauche des `:` dans le fichier `docker-compose.yml` (ex: `"8081:8080"` pour le backend).

### 2. Le backend ne démarre pas et indique une erreur de connexion à la base de données
*   **Explication :** Le conteneur backend a démarré trop vite avant que PostgreSQL n'ait fini de s'initialiser.
*   **Solution :** Nous avons configuré une condition `service_healthy` pour l'éviter, mais si cela arrive, redémarrez simplement le backend :
    ```bash
    docker compose restart backend
    ```

# 🐳 Guide de Démarrage avec Docker — SmartFleet Backend

Ce guide explique comment lancer rapidement l'ensemble des services du backend SmartFleet (Base de données PostGIS, moteur d'itinéraires Valhalla et API Spring Boot) en utilisant Docker.

---

## 📋 Prérequis
*   Avoir **Docker Desktop** (Windows / macOS) ou **Docker Engine** (Linux) installé et démarré.

---

## 🚀 Étape 1 : Lancement de l'Application

Ouvrez un terminal dans le dossier racine du backend (là où se trouve le fichier `docker-compose.yml`) et exécutez la commande :

```bash
docker compose up --build -d
```

> [!IMPORTANT]
> **Premier Démarrage (Valhalla) :**  
> Lors du tout premier lancement, le conteneur Valhalla va télécharger les données routières du Maroc (~200 Mo) et compiler les tuiles de calcul. Cette étape peut prendre entre **2 et 5 minutes** selon votre connexion et votre processeur. Les démarrages suivants seront instantanés.

---

## 🔍 Étape 2 : Vérification du Statut

1. **Vérifier que les conteneurs tournent :**
   ```bash
   docker compose ps
   ```
   *Tous les services (`smartfleet-db`, `smartfleet-valhalla`, `smartfleet-backend`) doivent être à l'état `running` (sain).*

2. **Consulter les logs du backend (Spring Boot) :**
   ```bash
   docker compose logs -f backend
   ```
   *Attendez de voir s'afficher la bannière de démarrage de Spring Boot et la confirmation d'écoute sur le port 8080.*

3. **Tester l'état de l'API (Health Check) :**  
   Ouvrez votre navigateur sur : [http://localhost:8080/api/health](http://localhost:8080/api/health).  
   Vous devriez recevoir : `{"status":"UP", "service":"SmartFleet Backend", ...}`.

---

## 🔌 Adresses utiles pour le test

*   **Documentation interactive (Swagger UI) :** [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html)
*   **Documentation API brute (OpenAPI JSON) :** [http://localhost:8080/api/v3/api-docs](http://localhost:8080/api/v3/api-docs)
*   **Accès Base de Données (PostgreSQL + PostGIS) :**
    *   **Hôte :** `localhost` (Port : `5432`)
    *   **Utilisateur :** `smartfleet_user`
    *   **Mot de passe :** `smartfleet_password`
    *   **Base de données :** `smartfleet_db_dev`

---

## 🛑 Étape 3 : Arrêt des services

*   **Arrêter les conteneurs (en conservant les données de la base) :**
    ```bash
    docker compose down
    ```
*   **Arrêter les conteneurs et supprimer les données (réinitialisation complète) :**
    ```bash
    docker compose down -v
    ```

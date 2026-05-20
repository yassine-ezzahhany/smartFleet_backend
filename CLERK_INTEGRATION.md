# Intégration Clerk - Guide Complet

Ce guide explique comment configurer et utiliser l'authentification Clerk avec le backend SmartFleet.

## Table des matières
1. [Configuration initiale](#configuration-initiale)
2. [Architecture](#architecture)
3. [Endpoints API](#endpoints-api)
4. [Webhooks Clerk](#webhooks-clerk)
5. [Frontend Integration](#frontend-integration)
6. [Dépannage](#dépannage)

---

## Configuration initiale

### 1. Créer un compte Clerk
- Allez sur [clerk.com](https://clerk.com) et créez un compte
- Créez une nouvelle application

### 2. Récupérer les credentials Clerk
Dans le dashboard Clerk, aller à **Settings > API Keys** et copier:
- **Publishable Key** (côté frontend)
- **Secret Key** (côté backend, non utilisé directement dans ce projet)

### 3. Configurer les variables d'environnement

Créer un fichier `.env` à la racine du projet backend en copiant `.env.example`:

```bash
cp .env.example .env
```

Puis remplir les valeurs Clerk:

```properties
CLERK_ISSUER=https://your-instance.clerk.accounts.com
CLERK_AUDIENCE=https://your-api.example.com
CLERK_WEBHOOK_SECRET=whsec_xxx_your_webhook_secret_here
```

### 4. Configurer les URL autorisées dans Clerk

1. Allez à **Settings > URLs** dans le dashboard Clerk
2. Ajouter l'URL de votre application (ex: `http://localhost:3000` pour dev)
3. Ajouter l'URL du webhook webhook (ex: `http://localhost:8080/api/webhooks/clerk/events`)

### 5. Configurer le Webhook Clerk

1. Allez à **Settings > Webhooks** dans le dashboard Clerk
2. Créer un nouveau webhook avec les paramètres suivants:
   - **Endpoint URL**: `https://votre-domaine.com/api/webhooks/clerk/events`
   - **Events à surveiller**:
     - `user.created`
     - `user.updated`
     - `user.deleted`
3. Copier le **Signing Secret** et le mettre dans `CLERK_WEBHOOK_SECRET`

---

## Architecture

### Flux d'authentification

```
Frontend (avec Clerk SDK)
    ↓
[L'utilisateur se connecte via Clerk UI]
    ↓
Clerk envoie un JWT au frontend
    ↓
Frontend inclut le JWT dans les headers (Authorization: Bearer <token>)
    ↓
Backend Spring Boot
    ↓
SecurityConfig valide le JWT via le JWKS de Clerk
    ↓
Request autorisée ✓
```

### Composants implémentés

1. **UserService** (`service/UserService.java`)
   - Gère la création/récupération des utilisateurs
   - Synchronise les données depuis Clerk
   - Gère la déactivation

2. **AuthController** (`controller/AuthController.java`)
   - `/auth/me` - Récupère le profil utilisateur courant
   - `/auth/callback` - Callback après authentification
   - `/auth/me` (PUT) - Met à jour le profil

3. **ClerkWebhookController** (`controller/ClerkWebhookController.java`)
   - Reçoit les événements Clerk
   - Synchronise les utilisateurs avec la base de données

4. **SecurityConfig** (`config/SecurityConfig.java`)
   - Configure OAuth2 Resource Server
   - Valide les JWT Clerk
   - Configure CORS et CSRF

---

## Endpoints API

### Publics (sans authentification)
```
GET  /api/health                    # Health check
GET  /api/auth/health               # Auth service health
POST /api/webhooks/clerk/events     # Webhooks Clerk (signature vérifiée)
```

### Protégés (nécessitent un JWT valide)
```
GET    /api/auth/me                 # Récupère le profil utilisateur
PUT    /api/auth/me                 # Met à jour le profil
POST   /api/auth/callback           # Callback après authentification
```

### Exemple de requête avec authentification

```bash
curl -H "Authorization: Bearer <jwt_token>" \
     http://localhost:8080/api/auth/me
```

Réponse:
```json
{
  "id": 1,
  "clerkId": "user_xxxxx",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "role": "DRIVER",
  "active": true
}
```

---

## Webhooks Clerk

### Événements gérés

#### 1. `user.created`
Quand un nouvel utilisateur se crée dans Clerk:
- L'utilisateur est créé dans la base de données SmartFleet
- Rôle par défaut: `DRIVER`

#### 2. `user.updated`
Quand l'utilisateur met à jour son profil dans Clerk:
- Les informations sont synchronisées dans SmartFleet

#### 3. `user.deleted`
Quand l'utilisateur supprime son compte dans Clerk:
- L'utilisateur est marqué comme inactif dans SmartFleet (soft delete)

### Structure de l'événement

```json
{
  "type": "user.created",
  "data": {
    "id": "user_xxxxx",
    "email_addresses": [
      {
        "email_address": "user@example.com"
      }
    ],
    "first_name": "John",
    "last_name": "Doe"
  }
}
```

---

## Frontend Integration

### Installation du SDK Clerk (React/Vue/Angular)

```bash
npm install @clerk/clerk-react
# ou
npm install @clerk/clerk-vue
# ou
npm install @clerk/clerk-angular
```

### Exemple avec React

```jsx
import { ClerkProvider, SignedIn, SignedOut } from "@clerk/clerk-react";

function App() {
  return (
    <ClerkProvider publishableKey={import.meta.env.VITE_CLERK_PUBLISHABLE_KEY}>
      <SignedOut>
        <SignIn />
      </SignedOut>
      <SignedIn>
        <Dashboard />
      </SignedIn>
    </ClerkProvider>
  );
}
```

### Appels API avec authentification

```jsx
import { useAuth } from "@clerk/clerk-react";

function Profile() {
  const { getToken } = useAuth();

  async function fetchProfile() {
    const token = await getToken();
    const response = await fetch("/api/auth/me", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    return await response.json();
  }

  // ...
}
```

---

## Dépannage

### Dépendances Clerk

**Note importante**: Ce projet n'utilise pas la dépendance officielle Clerk SDK car elle n'est pas disponible en Maven Central. À la place, nous utilisons:
- **Spring OAuth2 Resource Server** - pour valider les JWT
- **JJWT** - pour la manipulation des tokens JWT
- **Jackson** - pour traiter les webhooks JSON

Cette approche est suffisante pour une intégration complète de Clerk et évite les problèmes de dépendances.

### Le JWT n'est pas validé
1. Vérifier que `CLERK_ISSUER` est correct (sans trailing slash)
2. Vérifier que le JWKS endpoint est accessible: `https://your-instance.clerk.accounts.com/.well-known/jwks.json`
3. Vérifier les logs Spring pour les erreurs JWT

### Les webhooks ne reçoivent pas d'événements
1. Vérifier que l'URL du webhook est correctement configurée dans Clerk
2. Vérifier que le webhook est public (pas derrière un pare-feu)
3. Vérifier les logs des webhooks dans le dashboard Clerk

### L'utilisateur n'est pas créé dans la base de données
1. Vérifier que le webhook `user.created` est configuré
2. Vérifier les logs du backend pour les erreurs
3. Vérifier que `CLERK_WEBHOOK_SECRET` est correctement configuré

### La validation CORS échoue
1. Vérifier que l'URL frontend est autorisée dans `/api/webhooks/**`
2. Vérifier que l'URL du frontend est correctement configurée dans Clerk

---

## Variables d'environnement

| Variable | Description | Obligatoire |
|----------|-------------|-------------|
| `CLERK_ISSUER` | URL du provider Clerk | ✓ |
| `CLERK_AUDIENCE` | Audience JWT (votre API) | ✗ |
| `CLERK_WEBHOOK_SECRET` | Secret pour valider les webhooks | ✓ |
| `DB_URL` | URL de la base de données | ✓ |
| `DB_USERNAME` | Utilisateur BD | ✓ |
| `DB_PASSWORD` | Mot de passe BD | ✓ |

---

## Déploiement

### Production avec Docker

1. Copier le `.env` dans le container
2. Configurer les variables d'environnement dans le manifest Docker/Kubernetes
3. Mettre à jour l'URL du webhook dans Clerk avec le domaine de production
4. Vérifier les certificats SSL (important pour Clerk)

### Variables d'environnement en production

```properties
CLERK_ISSUER=https://your-prod-instance.clerk.accounts.com
CLERK_WEBHOOK_SECRET=whsec_prod_xxxxxx
DB_URL=postgresql://prod-user:prod-pass@prod-db.example.com:5432/smartfleet
```

---

## Ressources

- [Clerk Documentation](https://clerk.com/docs)
- [Clerk Java SDK](https://github.com/clerk/clerk-sdk-java)
- [Spring Security OAuth2](https://spring.io/projects/spring-security)
- [JJWT - JWT Library](https://github.com/jwtk/jjwt)

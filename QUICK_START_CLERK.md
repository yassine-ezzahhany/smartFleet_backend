# Quick Start - Intégration Clerk

Guide rapide pour mettre en place l'authentification Clerk.

## 1️⃣ Créer un compte Clerk

1. Allez sur [clerk.com](https://clerk.com)
2. Créez un compte et une application
3. Copiez vos credentials

## 2️⃣ Configurer les variables d'environnement

```bash
cp .env.example .env
```

Remplissez le fichier `.env`:
```properties
CLERK_ISSUER=https://your-instance.clerk.accounts.com
CLERK_AUDIENCE=https://your-api.example.com
CLERK_WEBHOOK_SECRET=whsec_your_secret_here
```

Pour trouver ces valeurs:
- **CLERK_ISSUER**: Dashboard Clerk > Settings > API Keys
- **CLERK_WEBHOOK_SECRET**: Dashboard Clerk > Settings > Webhooks (après création du webhook)

## 3️⃣ Configurer les URLs Clerk

1. Allez au Dashboard Clerk
2. Settings > URLs
3. Ajouter votre URL frontend (ex: `http://localhost:3000`)

## 4️⃣ Configurer le Webhook Clerk

1. Dashboard Clerk > Settings > Webhooks
2. Créer un webhook:
   - **Endpoint URL**: `http://localhost:8080/api/webhooks/clerk/events` (dev)
     - En production: `https://your-domain.com/api/webhooks/clerk/events`
   - **Events**: Sélectionner `user.created`, `user.updated`, `user.deleted`
3. Copier le **Signing Secret** dans `.env` en tant que `CLERK_WEBHOOK_SECRET`

## 5️⃣ Compiler et tester

```bash
# Compiler
./mvnw clean compile

# Lancer les tests
./mvnw test

# Démarrer le serveur
./mvnw spring-boot:run
```

Le serveur démarre sur `http://localhost:8080`

## 6️⃣ Tester les endpoints

### Sans authentification
```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/auth/health
```

### Avec authentification (Nécessite un JWT de Clerk)
```bash
# Depuis votre frontend, récupérez un token:
const token = await getToken();

# Puis appelez:
curl -H "Authorization: Bearer $token" \
     http://localhost:8080/api/auth/me
```

## 7️⃣ Frontend - Intégrer Clerk SDK

### Exemple React
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

## ✅ Endpoints disponibles

### Public
- `GET /api/health` - Health check
- `GET /api/auth/health` - Auth service health
- `POST /api/webhooks/clerk/events` - Webhooks Clerk

### Protected (nécessite JWT)
- `GET /api/auth/me` - Profil utilisateur
- `PUT /api/auth/me` - Mettre à jour profil
- `POST /api/auth/callback` - Callback post-auth

## 📚 Documentation complète

- **CLERK_INTEGRATION.md** - Guide détaillé (configuration, architecture, webhooks, etc.)
- **CLERK_SETUP.md** - Résumé des changements implémentés
- **.env.example** - Toutes les variables d'environnement disponibles

## 🐛 Problèmes courants

**JWT non valide**
- Vérifier que `CLERK_ISSUER` est correct
- Vérifier que le JWKS est accessible: `{CLERK_ISSUER}/.well-known/jwks.json`

**Webhooks non reçus**
- Vérifier que l'URL du webhook est publique (pas derrière un firewall)
- Vérifier le `CLERK_WEBHOOK_SECRET` dans les logs Clerk

**Utilisateur non créé dans la BD**
- Vérifier que l'événement `user.created` est activé dans Clerk
- Vérifier les logs du backend

## 🚀 Déploiement

1. Définir les variables d'environnement en production
2. Mettre à jour l'URL du webhook dans Clerk avec le domaine de prod
3. Vérifier les certificats SSL
4. Tester les webhooks en production

---

Pour plus de détails, consulter **CLERK_INTEGRATION.md**

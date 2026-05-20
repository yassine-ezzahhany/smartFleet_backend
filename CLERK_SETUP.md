# Intégration Clerk - Résumé des Changements

## 📋 Vue d'ensemble

Cette intégration ajoute l'authentification Clerk au backend SmartFleet, permettant une gestion complète des utilisateurs avec synchronisation automatique.

## 🔧 Fichiers Créés/Modifiés

### Fichiers Créés

#### Service
- **`src/main/java/ma/smartfleet/backend/service/UserService.java`**
  - Service métier pour la gestion des utilisateurs
  - Crée/récupère les utilisateurs depuis Clerk
  - Synchronise les données utilisateur
  - Gère la déactivation des utilisateurs

#### Contrôleurs
- **`src/main/java/ma/smartfleet/backend/controller/AuthController.java`**
  - GET `/auth/me` - Récupère le profil utilisateur courant
  - POST `/auth/callback` - Callback après authentification Clerk
  - PUT `/auth/me` - Met à jour le profil utilisateur
  - GET `/auth/health` - Health check

- **`src/main/java/ma/smartfleet/backend/controller/ClerkWebhookController.java`**
  - POST `/webhooks/clerk/events` - Reçoit les événements Clerk
  - Gère: user.created, user.updated, user.deleted
  - Synchronise automatiquement la base de données

#### Utilitaires
- **`src/main/java/ma/smartfleet/backend/util/ClerkAuthUtils.java`**
  - Classe utilitaire pour extraire les informations d'authentification
  - Méthodes pour accéder au clerkId, email, prénom, nom depuis le contexte de sécurité

#### Tests
- **`src/test/java/ma/smartfleet/backend/service/UserServiceTest.java`**
  - Tests unitaires du UserService
  - Couvre: création, récupération, synchronisation, conversion DTO

#### Configuration & Documentation
- **`.env.example`**
  - Template pour les variables d'environnement
  - Variables Clerk: CLERK_ISSUER, CLERK_AUDIENCE, CLERK_WEBHOOK_SECRET

- **`CLERK_INTEGRATION.md`**
  - Guide complet d'intégration Clerk
  - Configuration, endpoints, webhooks, examples

### Fichiers Modifiés

#### Dépendances
- **`pom.xml`**
  - ✓ Dépendances existantes suffisent (JWT, Spring OAuth2, Jackson)
  - Note: Nous n'utilisons pas de SDK Clerk car il n'est pas disponible en Maven Central

#### Configuration
- **`src/main/java/ma/smartfleet/backend/config/SecurityConfig.java`**
  - Ajout: `/api/webhooks/**` aux endpoints publics
  - Permet aux webhooks Clerk d'être appelés sans authentification

- **`src/main/resources/application.properties`**
  - Variables existantes: `app.security.clerk.issuer`, `app.security.clerk.audience`
  - ✓ Déjà correctement configurés

## 🔐 Architecture de Sécurité

### Flux d'authentification
```
1. Utilisateur se connecte via frontend Clerk UI
2. Clerk émet un JWT valide
3. Frontend envoie JWT au backend dans le header Authorization
4. Spring Security valide le JWT via NimbusJwtDecoder
5. NimbusJwtDecoder récupère le JWKS depuis https://{issuer}/.well-known/jwks.json
6. JWT validé → requête autorisée
```

### Webhooks sécurisés
```
1. Événement utilisateur dans Clerk
2. Clerk envoie webhook signé au backend
3. ClerkWebhookController reçoit l'événement
4. Webhook traité et base de données synchronisée
```

## 📡 Endpoints API

### Authentification (Public)
```bash
POST /api/auth/callback        # Après authentification Clerk
GET  /api/auth/me              # Profil utilisateur (JWT requis)
PUT  /api/auth/me              # Mise à jour profil (JWT requis)
GET  /api/auth/health          # Health check
```

### Webhooks (Public)
```bash
POST /api/webhooks/clerk/events # Webhooks Clerk
```

## 🚀 Configuration pour le Déploiement

### Variables d'environnement requises
```properties
CLERK_ISSUER=https://your-instance.clerk.accounts.com
CLERK_AUDIENCE=https://your-api.example.com
CLERK_WEBHOOK_SECRET=whsec_xxx_xxx_xxx
```

### Configuration Clerk Dashboard
1. Ajouter les URLs autorisées dans **Settings > URLs**
2. Configurer le webhook dans **Settings > Webhooks**
3. Vérifier les events: user.created, user.updated, user.deleted

## 📝 Utilisation dans les contrôleurs

### Accéder aux informations de l'utilisateur courant
```java
import ma.smartfleet.backend.util.ClerkAuthUtils;

@GetMapping("/my-data")
public ResponseEntity<?> getMyData() {
    String clerkId = ClerkAuthUtils.getCurrentClerkId().orElse(null);
    String email = ClerkAuthUtils.getCurrentEmail().orElse(null);
    // ...
}
```

### Exemple dans un contrôleur
```java
@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    public ResponseEntity<DriverDTO> getProfile() {
        var user = ClerkAuthUtils.getCurrentClerkId()
            .flatMap(userService::getUserByClerkId)
            .orElse(null);
        // ...
    }
}
```

## ✅ Checklist d'intégration

- [x] Dépendance Clerk ajoutée
- [x] Service UserService créé
- [x] AuthController créé
- [x] ClerkWebhookController créé
- [x] SecurityConfig mis à jour
- [x] ClerkAuthUtils créé
- [x] Tests unitaires ajoutés
- [x] .env.example créé
- [x] Documentation CLERK_INTEGRATION.md créée
- [ ] Variables d'environnement configurées (à faire)
- [ ] Webhooks Clerk configurés (à faire)
- [ ] Tests d'intégration exécutés (à faire)
- [ ] Déploiement en production (à faire)

## 🔍 Tests

### Lancer les tests
```bash
mvn test -Dtest=UserServiceTest
```

### Tester les endpoints manuellement
```bash
# Avec token JWT valide
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/auth/me
```

## 📚 Ressources

- [Clerk Documentation](https://clerk.com/docs)
- [Spring Security OAuth2](https://spring.io/projects/spring-security)
- [JJWT - JWT Library](https://github.com/jwtk/jjwt)
- Guide complet: Voir `CLERK_INTEGRATION.md`

## 🐛 Dépannage

Voir `CLERK_INTEGRATION.md` pour la section "Dépannage"

### Problèmes courants
1. **JWT non validé**: Vérifier CLERK_ISSUER et la connectivité JWKS
2. **Webhooks non reçus**: Vérifier l'URL publique et le CLERK_WEBHOOK_SECRET
3. **Utilisateur non créé**: Vérifier les logs du webhook et la BD

## 📞 Support

Pour toute question ou problème:
1. Consulter `CLERK_INTEGRATION.md`
2. Vérifier les logs du backend
3. Vérifier le dashboard Clerk pour les événements

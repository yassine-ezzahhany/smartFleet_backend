package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.util.ClerkAuthUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Exemple de contrôleur montrant comment utiliser l'authentification Clerk.
 * Ce contrôleur est fourni à titre d'exemple et peut être supprimé.
 */
@RestController
@RequestMapping("/example")
@RequiredArgsConstructor
public class ExampleAuthController {

    /**
     * Endpoint qui démontre l'accès aux informations d'authentification Clerk.
     * Nécessite un JWT valide.
     */
    @GetMapping("/auth-info")
    public ResponseEntity<Map<String, Object>> getAuthInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Récupérer les informations du JWT
        ClerkAuthUtils.getCurrentClerkId().ifPresent(id -> info.put("clerkId", id));
        ClerkAuthUtils.getCurrentEmail().ifPresent(email -> info.put("email", email));
        ClerkAuthUtils.getCurrentFirstName().ifPresent(firstName -> info.put("firstName", firstName));
        ClerkAuthUtils.getCurrentLastName().ifPresent(lastName -> info.put("lastName", lastName));
        ClerkAuthUtils.getFullJwt().ifPresent(jwt -> {
            info.put("issuer", jwt.getIssuer());
            info.put("audience", jwt.getAudience());
            info.put("expiresAt", jwt.getExpiresAt());
        });
        
        info.put("authenticated", ClerkAuthUtils.isAuthenticated());
        
        return ResponseEntity.ok(info);
    }

    /**
     * Endpoint qui montre comment accéder au clerkId directement.
     */
    @GetMapping("/my-clerk-id")
    public ResponseEntity<Map<String, String>> getMyClerkId() {
        return ClerkAuthUtils.getCurrentClerkId()
            .map(clerkId -> ResponseEntity.ok(Map.of("clerkId", clerkId)))
            .orElse(ResponseEntity.status(401).build());
    }

    /**
     * Endpoint sécurisé qui utilise les informations de l'utilisateur.
     */
    @GetMapping("/secure-data")
    public ResponseEntity<Map<String, String>> getSecureData() {
        var clerkId = ClerkAuthUtils.getCurrentClerkId().orElse(null);
        var email = ClerkAuthUtils.getCurrentEmail().orElse(null);
        
        if (clerkId == null) {
            return ResponseEntity.status(401).build();
        }
        
        Map<String, String> data = new HashMap<>();
        data.put("message", "Bienvenue " + email);
        data.put("clerkId", clerkId);
        
        return ResponseEntity.ok(data);
    }
}

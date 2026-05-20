package ma.smartfleet.backend.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * Utilitaire pour extraire les informations de l'authentification Clerk.
 */
public class ClerkAuthUtils {

    /**
     * Récupère le clerkId depuis le contexte de sécurité courant.
     */
    public static Optional<String> getCurrentClerkId() {
        return getJwt().map(Jwt::getSubject);
    }

    /**
     * Récupère l'email depuis le JWT.
     */
    public static Optional<String> getCurrentEmail() {
        return getJwt().map(jwt -> jwt.getClaimAsString("email"));
    }

    /**
     * Récupère le prénom depuis le JWT.
     */
    public static Optional<String> getCurrentFirstName() {
        return getJwt().map(jwt -> jwt.getClaimAsString("given_name"));
    }

    /**
     * Récupère le nom de famille depuis le JWT.
     */
    public static Optional<String> getCurrentLastName() {
        return getJwt().map(jwt -> jwt.getClaimAsString("family_name"));
    }

    /**
     * Récupère la liste des rôles depuis le JWT (si présente).
     */
    public static Optional<Object> getCurrentRoles() {
        return getJwt().map(jwt -> jwt.getClaimAsString("roles"));
    }

    /**
     * Récupère le JWT du contexte de sécurité courant.
     */
    private static Optional<Jwt> getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return Optional.empty();
        }
        return Optional.of((Jwt) authentication.getPrincipal());
    }

    /**
     * Vérifie si l'utilisateur est authentifié.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Récupère le JWT complet (pour les cas avancés).
     */
    public static Optional<Jwt> getFullJwt() {
        return getJwt();
    }
}

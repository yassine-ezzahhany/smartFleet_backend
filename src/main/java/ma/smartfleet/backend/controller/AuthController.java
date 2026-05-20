package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.dto.UserDTO;
import ma.smartfleet.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Endpoint pour récupérer les informations du profil utilisateur courant.
     * Extrait les données du JWT de Clerk et retourne le profil utilisateur.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return ResponseEntity.notFound().build();
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String clerkId = jwt.getSubject();
        
        return userService.getUserByClerkId(clerkId)
            .map(user -> ResponseEntity.ok(userService.convertToDTO(user)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint appelé lors de la première connexion (callback après authentification Clerk).
     * Crée ou récupère l'utilisateur et retourne ses informations.
     */
    @PostMapping("/callback")
    public ResponseEntity<UserDTO> authCallback() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return ResponseEntity.badRequest().build();
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String clerkId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        var user = userService.getOrCreateUserFromClerk(clerkId, email, firstName, lastName);
        return ResponseEntity.ok(userService.convertToDTO(user));
    }

    /**
     * Endpoint pour mettre à jour le profil utilisateur.
     */
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateProfile(@RequestBody UserDTO userDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return ResponseEntity.notFound().build();
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String clerkId = jwt.getSubject();

        var user = userService.getUserByClerkId(clerkId)
            .orElse(null);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        var updatedUser = userService.updateUser(user.getId(), userDTO);
        return ResponseEntity.ok(userService.convertToDTO(updatedUser));
    }

    /**
     * Health check pour vérifier que l'authentification fonctionne.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "Auth Service"));
    }
}

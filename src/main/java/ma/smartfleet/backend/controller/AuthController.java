package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.LoginRequestDTO;
import ma.smartfleet.backend.dto.LoginResponseDTO;
import ma.smartfleet.backend.dto.RegisterRequestDTO;
import ma.smartfleet.backend.dto.UserDTO;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.service.UserService;
import ma.smartfleet.backend.util.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Enregistre un nouvel utilisateur localement.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            User registeredUser = userService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.convertToDTO(registeredUser));
        } catch (IllegalArgumentException e) {
            log.warn("Tentative d'inscription invalide : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de l'inscription :", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Une erreur est survenue lors de la création du compte."));
        }
    }

    /**
     * Authentifie l'utilisateur et retourne un token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            User user = userService.getUserByEmail(loginRequest.getEmail())
                    .orElse(null);

            if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Email ou mot de passe incorrect."));
            }

            if (!user.getActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Ce compte a été désactivé."));
            }

            String token = tokenProvider.generateToken(user);
            UserDTO userDTO = userService.convertToDTO(user);

            log.info("Connexion réussie pour l'utilisateur : {}", user.getEmail());
            return ResponseEntity.ok(new LoginResponseDTO(token, userDTO));
        } catch (Exception e) {
            log.error("Erreur lors de l'authentification :", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Une erreur interne est survenue lors de la connexion."));
        }
    }

    /**
     * Retourne les informations du profil utilisateur courant.
     * Extrait les données de l'utilisateur authentifié depuis le contexte de sécurité.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.convertToDTO(user));
    }

    /**
     * Met à jour le profil de l'utilisateur connecté.
     */
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateProfile(@RequestBody UserDTO userDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) authentication.getPrincipal();
        var updatedUser = userService.updateUser(user.getId(), userDTO);
        return ResponseEntity.ok(userService.convertToDTO(updatedUser));
    }

    /**
     * Health check pour vérifier le bon état de marche de l'API auth.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "Auth Service Local"));
    }
}

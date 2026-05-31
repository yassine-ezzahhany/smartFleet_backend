package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.dto.DriverLocationUpdateDTO;
import ma.smartfleet.backend.dto.UserDTO;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.model.enums.UserRole;
import ma.smartfleet.backend.service.LocationTrackingService;
import ma.smartfleet.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final LocationTrackingService locationTrackingService;
    private final UserService userService;

    /**
     * Reçoit la mise à jour de position GPS d'un conducteur.
     * Appelé par l'app mobile toutes les 30 secondes.
     */
    @PutMapping("/{id}/location")
    public ResponseEntity<Void> updateLocation(@PathVariable Long id,
                                               @RequestBody DriverLocationUpdateDTO dto) {
        locationTrackingService.updateDriverLocation(id, dto.getLatitude(), dto.getLongitude(), dto.getTimestamp());
        return ResponseEntity.ok().build();
    }

    /**
     * Vérifie si un conducteur est proche d'un point de livraison.
     */
    @GetMapping("/{id}/nearby")
    public ResponseEntity<Map<String, Boolean>> isNearby(@PathVariable Long id,
                                                          @RequestParam Double lat,
                                                          @RequestParam Double lon,
                                                          @RequestParam(defaultValue = "1.0") Double radiusKm) {
        boolean nearby = locationTrackingService.isDriverNearby(id, lat, lon, radiusKm);
        return ResponseEntity.ok(Map.of("nearby", nearby));
    }

    /**
     * Liste tous les chauffeurs qui ne sont pas encore affectés à un manager (manager_id = null).
     */
    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<UserDTO>> getUnassignedDrivers() {
        return ResponseEntity.ok(userService.getUnassignedDrivers());
    }

    /**
     * Liste de tous les chauffeurs affectés au manager connecté.
     */
    @GetMapping("/my-drivers")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<UserDTO>> getMyDrivers() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(userService.getDriversByManager(user.getId()));
    }

    /**
     * Affecte un chauffeur (manager_id = manager connecté).
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserDTO> assignDriver(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserDTO updated = userService.assignDriverToManager(id, user.getId());
        return ResponseEntity.ok(updated);
    }

    /**
     * Désaffecte un chauffeur de la flotte du manager connecté (manager_id = null).
     */
    @PostMapping("/{id}/unassign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserDTO> unassignDriver(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            UserDTO updated = userService.removeDriverFromManager(id, user.getId());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new IllegalStateException("Utilisateur non authentifié.");
        }
        return (User) authentication.getPrincipal();
    }
}

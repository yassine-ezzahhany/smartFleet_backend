package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.VehicleDTO;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.model.Vehicle;
import ma.smartfleet.backend.model.enums.UserRole;
import ma.smartfleet.backend.service.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * Ajoute un nouveau véhicule à la flotte du Manager connecté.
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<VehicleDTO> createVehicle(@RequestBody VehicleDTO vehicleDTO) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Vehicle vehicle = vehicleService.addVehicle(vehicleDTO, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.convertToDTO(vehicle));
    }

    /**
     * Récupère la liste de tous les véhicules du Manager connecté.
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<VehicleDTO>> getMyVehicles() {
        User user = getAuthenticatedUser();
        List<VehicleDTO> vehicles = vehicleService.getVehiclesByManager(user.getId());
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Récupère uniquement les véhicules actifs du Manager connecté.
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<VehicleDTO>> getMyActiveVehicles() {
        User user = getAuthenticatedUser();
        List<VehicleDTO> vehicles = vehicleService.getActiveVehiclesByManager(user.getId());
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Met à jour les caractéristiques d'un véhicule spécifique de la flotte du Manager.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<VehicleDTO> updateVehicle(@PathVariable Long id, @RequestBody VehicleDTO vehicleDTO) {
        User user = getAuthenticatedUser();
        
        // S'assurer que le véhicule appartient bien au manager connecté
        List<VehicleDTO> myVehicles = vehicleService.getVehiclesByManager(user.getId());
        boolean ownsVehicle = myVehicles.stream().anyMatch(v -> v.getId().equals(id));
        if (!ownsVehicle) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Vehicle vehicle = vehicleService.updateVehicle(id, vehicleDTO);
        return ResponseEntity.ok(vehicleService.convertToDTO(vehicle));
    }

    /**
     * Désactive ou réactive un véhicule de la flotte.
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> toggleVehicleActive(@PathVariable Long id, @RequestParam boolean active) {
        User user = getAuthenticatedUser();

        // S'assurer que le véhicule appartient au manager connecté
        List<VehicleDTO> myVehicles = vehicleService.getVehiclesByManager(user.getId());
        boolean ownsVehicle = myVehicles.stream().anyMatch(v -> v.getId().equals(id));
        if (!ownsVehicle) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        vehicleService.toggleVehicleActive(id, active);
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new IllegalStateException("Utilisateur non authentifié.");
        }
        return (User) authentication.getPrincipal();
    }
}

package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.dto.DriverLocationUpdateDTO;
import ma.smartfleet.backend.service.LocationTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final LocationTrackingService locationTrackingService;

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
}

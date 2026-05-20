package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.service.DeliveryOptimizationService;
import ma.smartfleet.backend.service.DeliveryOptimizationService.OptimizationStats;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/optimization")
@RequiredArgsConstructor
public class OptimizationController {

    private final DeliveryOptimizationService optimizationService;

    /**
     * Lance l'optimisation d'un programme de livraison et crée les sous-programmes.
     */
    @PostMapping("/programs/{id}")
    public ResponseEntity<Void> optimizeProgram(@PathVariable Long id) {
        optimizationService.optimizeProgram(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Retourne les statistiques d'un programme de livraison optimisé.
     */
    @GetMapping("/programs/{id}/stats")
    public ResponseEntity<OptimizationStats> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(optimizationService.getStats(id));
    }
}

package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.dto.DeliveryProgramDTO;
import ma.smartfleet.backend.model.DeliveryProgram;
import ma.smartfleet.backend.service.DeliveryOptimizationService;
import ma.smartfleet.backend.service.DeliveryOptimizationService.OptimizationStats;
import ma.smartfleet.backend.service.DeliveryProgramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/optimization")
@RequiredArgsConstructor
public class OptimizationController {

    private final DeliveryOptimizationService optimizationService;
    private final DeliveryProgramService deliveryProgramService;

    /**
     * Lance l'optimisation d'un programme de livraison et crée les sous-programmes.
     */
    @PostMapping("/programs/{id}")
    public ResponseEntity<DeliveryProgramDTO> optimizeProgram(@PathVariable Long id) {
        DeliveryProgram program = optimizationService.optimizeProgram(id);
        return ResponseEntity.ok(deliveryProgramService.convertToDTO(program));
    }

    /**
     * Retourne les statistiques d'un programme de livraison optimisé.
     */
    @GetMapping("/programs/{id}/stats")
    public ResponseEntity<OptimizationStats> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(optimizationService.getStats(id));
    }
}

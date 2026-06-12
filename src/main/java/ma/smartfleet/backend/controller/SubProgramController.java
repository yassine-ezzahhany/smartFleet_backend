package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.dto.SubProgramDTO;
import ma.smartfleet.backend.exception.ResourceNotFoundException;
import ma.smartfleet.backend.model.SubProgram;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.repository.SubProgramRepository;
import ma.smartfleet.backend.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subprograms")
@RequiredArgsConstructor
public class SubProgramController {

    private final SubProgramRepository subProgramRepository;
    private final RouteService routeService;

    @GetMapping("/{id}")
    public ResponseEntity<SubProgramDTO> getSubProgram(@PathVariable Long id) {
        SubProgram sp = subProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubProgram", id));
        return ResponseEntity.ok(toDto(sp));
    }

    /**
     * Déclenche le calcul de route Valhalla pour un sous-programme.
     */
    @PostMapping("/{id}/calculate-route")
    public ResponseEntity<SubProgramDTO> calculateRoute(@PathVariable Long id) {
        SubProgram sp = routeService.calculateOptimalRoute(id);
        return ResponseEntity.ok(toDto(sp));
    }

    /**
     * Démarre un sous-programme : change le statut du sous-programme en IN_PROGRESS,
     * et met à jour toutes ses commandes associées au statut IN_TRANSIT.
     */
    @PutMapping("/{id}/start")
    @Transactional
    public ResponseEntity<SubProgramDTO> startSubProgram(@PathVariable Long id) {
        SubProgram sp = subProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubProgram", id));
        
        sp.setStatus(ma.smartfleet.backend.model.enums.SubProgramStatus.IN_TRANSIT);
        sp.setStartTime(java.time.LocalDateTime.now());
        
        if (sp.getOrders() != null) {
            for (ma.smartfleet.backend.model.Order order : sp.getOrders()) {
                order.setStatus(ma.smartfleet.backend.model.enums.OrderStatus.IN_TRANSIT);
            }
        }
        
        SubProgram saved = subProgramRepository.save(sp);
        return ResponseEntity.ok(toDto(saved));
    }

    /**
     * Récupère le sous-programme actif (ASSIGNED ou IN_PROGRESS) du chauffeur connecté.
     */
    @GetMapping("/my-active")
    public ResponseEntity<SubProgramDTO> getMyActiveSubProgram() {
        User user = getAuthenticatedUser();
        List<SubProgram> activeSubPrograms = subProgramRepository.findByDriverIdAndStatus(user.getId(), ma.smartfleet.backend.model.enums.SubProgramStatus.ASSIGNED);
        if (activeSubPrograms.isEmpty()) {
            activeSubPrograms = subProgramRepository.findByDriverIdAndStatus(user.getId(), ma.smartfleet.backend.model.enums.SubProgramStatus.IN_TRANSIT);
        }
        if (activeSubPrograms.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(activeSubPrograms.get(0)));
    }

    /**
     * Récupère la liste de tous les sous-programmes affectés au chauffeur connecté.
     */
    @GetMapping("/my-subprograms")
    public ResponseEntity<List<SubProgramDTO>> getMySubPrograms() {
        User user = getAuthenticatedUser();
        List<SubProgram> list = subProgramRepository.findByDriverId(user.getId());
        return ResponseEntity.ok(list.stream().map(this::toDto).collect(Collectors.toList()));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new IllegalStateException("Utilisateur non authentifié.");
        }
        return (User) authentication.getPrincipal();
    }

    private SubProgramDTO toDto(SubProgram sp) {
        SubProgramDTO dto = new SubProgramDTO();
        dto.setId(sp.getId());
        dto.setSubProgramNumber(sp.getSubProgramNumber());
        dto.setDeliveryProgramId(sp.getDeliveryProgram() != null ? sp.getDeliveryProgram().getId() : null);
        dto.setDriverId(sp.getDriver() != null ? sp.getDriver().getId() : null);
        dto.setVehicleId(sp.getVehicle() != null ? sp.getVehicle().getId() : null);
        dto.setStatus(sp.getStatus() != null ? sp.getStatus().name() : null);
        dto.setPolyline(sp.getPolyline());
        dto.setEstimatedDistanceKm(sp.getEstimatedDistanceKm());
        dto.setEstimatedDurationMinutes(sp.getEstimatedDurationMinutes());
        dto.setActualDistanceKm(sp.getActualDistanceKm());
        dto.setActualDurationMinutes(sp.getActualDurationMinutes());
        dto.setTotalOrdersCount(sp.getTotalOrdersCount());
        dto.setApprovedOrdersCount(sp.getApprovedOrdersCount());
        dto.setStartTime(sp.getStartTime() != null ? sp.getStartTime().toString() : null);
        dto.setEndTime(sp.getEndTime() != null ? sp.getEndTime().toString() : null);
        if (sp.getOrders() != null) {
            dto.setOrderIds(sp.getOrders().stream().map(o -> o.getId()).collect(Collectors.toList()));
        }
        return dto;
    }
}

package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.dto.SubProgramDTO;
import ma.smartfleet.backend.exception.ResourceNotFoundException;
import ma.smartfleet.backend.model.SubProgram;
import ma.smartfleet.backend.repository.SubProgramRepository;
import ma.smartfleet.backend.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

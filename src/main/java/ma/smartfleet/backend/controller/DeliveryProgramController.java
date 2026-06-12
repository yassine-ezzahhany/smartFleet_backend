package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.DeliveryProgramDTO;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.service.DeliveryProgramService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
@Slf4j
public class DeliveryProgramController {

    private final DeliveryProgramService deliveryProgramService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DeliveryProgramDTO> createProgram(@RequestBody DeliveryProgramDTO dto) {
        User user = getAuthenticatedUser();
        DeliveryProgramDTO created = deliveryProgramService.createProgram(dto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<DeliveryProgramDTO>> getMyPrograms(@RequestParam(required = false) String status) {
        User user = getAuthenticatedUser();
        List<DeliveryProgramDTO> programs = deliveryProgramService.getProgramsByManager(user.getId(), status);
        return ResponseEntity.ok(programs);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DeliveryProgramDTO> getProgram(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        DeliveryProgramDTO program = deliveryProgramService.getProgramById(id, user.getId());
        return ResponseEntity.ok(program);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DeliveryProgramDTO> updateProgram(@PathVariable Long id, @RequestBody DeliveryProgramDTO dto) {
        User user = getAuthenticatedUser();
        DeliveryProgramDTO updated = deliveryProgramService.updateProgram(id, dto, user.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteProgram(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        deliveryProgramService.deleteProgram(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/orders")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DeliveryProgramDTO> addOrdersToProgram(@PathVariable Long id, @RequestBody List<Long> orderIds) {
        User user = getAuthenticatedUser();
        DeliveryProgramDTO updated = deliveryProgramService.addOrdersToProgram(id, orderIds, user.getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/orders/{orderId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DeliveryProgramDTO> removeOrderFromProgram(@PathVariable Long id, @PathVariable Long orderId) {
        User user = getAuthenticatedUser();
        DeliveryProgramDTO updated = deliveryProgramService.removeOrderFromProgram(id, orderId, user.getId());
        return ResponseEntity.ok(updated);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new IllegalStateException("Utilisateur non authentifié.");
        }
        return (User) authentication.getPrincipal();
    }
}

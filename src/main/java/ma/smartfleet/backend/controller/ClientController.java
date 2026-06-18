package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.ClientDTO;
import ma.smartfleet.backend.service.ClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public ResponseEntity<List<ClientDTO>> getAllClients() {
        log.info("Getting all clients");
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable Long id) {
        log.info("Getting client by id: {}", id);
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    
}

package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.ClientDTO;
import ma.smartfleet.backend.model.Client;
import ma.smartfleet.backend.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public List<ClientDTO> getAllClients() {
        List<Client> clients = clientRepository.findAll();
        return clients.stream()
                .map(ClientService::toDTO)
                .collect(Collectors.toList());
    }

    public ClientDTO getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
        return toDTO(client);
    }

    public static ClientDTO toDTO(Client client) {
        if (client == null) return null;
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setEmail(client.getEmail());
        dto.setName(client.getName());
        dto.setPhone(client.getPhone());
        dto.setCompanyName(client.getCompanyName());
        dto.setBusinessAddress(client.getBusinessAddress());
        dto.setBusinessPhone(client.getBusinessPhone());
        return dto;
    }

    public static Client toEntity(ClientDTO dto) {
        if (dto == null) return null;
        Client client = new Client();
        client.setId(dto.getId());
        client.setEmail(dto.getEmail());
        client.setName(dto.getName());
        client.setPhone(dto.getPhone());
        client.setCompanyName(dto.getCompanyName());
        client.setBusinessAddress(dto.getBusinessAddress());
        client.setBusinessPhone(dto.getBusinessPhone());
        return client;
    }
}

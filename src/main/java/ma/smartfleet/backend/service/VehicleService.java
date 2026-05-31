package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.VehicleDTO;
import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.model.Manager;
import ma.smartfleet.backend.model.Vehicle;
import ma.smartfleet.backend.repository.ManagerRepository;
import ma.smartfleet.backend.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ManagerRepository managerRepository;

    /**
     * Enregistre un nouveau véhicule avec ses caractéristiques pour un Manager donné.
     */
    public Vehicle addVehicle(VehicleDTO dto, Long managerId) {
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new OptimizationException("Manager non trouvé avec l'id : " + managerId));

        if (vehicleRepository.findByRegistrationNumber(dto.getRegistrationNumber()).isPresent()) {
            throw new IllegalArgumentException("Un véhicule avec ce numéro d'immatriculation existe déjà.");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber(dto.getRegistrationNumber());
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setYear(dto.getYear());
        vehicle.setMaxVolumeM2(dto.getMaxVolumeM2());
        vehicle.setMaxPayloadKg(dto.getMaxPayloadKg());
        vehicle.setCurrentLoadM2(0.0);
        vehicle.setCurrentLoadKg(0.0);
        vehicle.setManager(manager);
        vehicle.setActive(dto.getActive() != null ? dto.getActive() : true);

        log.info("Création d'un véhicule pour le manager {} : immatriculation {}", manager.getEmail(), vehicle.getRegistrationNumber());
        return vehicleRepository.save(vehicle);
    }

    /**
     * Récupère la liste des véhicules d'un Manager.
     */
    @Transactional(readOnly = true)
    public List<VehicleDTO> getVehiclesByManager(Long managerId) {
        return vehicleRepository.findByManagerId(managerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère la liste des véhicules actifs d'un Manager.
     */
    @Transactional(readOnly = true)
    public List<VehicleDTO> getActiveVehiclesByManager(Long managerId) {
        return vehicleRepository.findByManagerIdAndActiveTrue(managerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour les caractéristiques d'un véhicule.
     */
    public Vehicle updateVehicle(Long vehicleId, VehicleDTO dto) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new OptimizationException("Véhicule non trouvé avec l'id : " + vehicleId));

        if (dto.getRegistrationNumber() != null && !dto.getRegistrationNumber().equals(vehicle.getRegistrationNumber())) {
            if (vehicleRepository.findByRegistrationNumber(dto.getRegistrationNumber()).isPresent()) {
                throw new IllegalArgumentException("Un véhicule avec ce numéro d'immatriculation existe déjà.");
            }
            vehicle.setRegistrationNumber(dto.getRegistrationNumber());
        }

        if (dto.getBrand() != null) vehicle.setBrand(dto.getBrand());
        if (dto.getModel() != null) vehicle.setModel(dto.getModel());
        if (dto.getYear() != null) vehicle.setYear(dto.getYear());
        if (dto.getMaxVolumeM2() != null) vehicle.setMaxVolumeM2(dto.getMaxVolumeM2());
        if (dto.getMaxPayloadKg() != null) vehicle.setMaxPayloadKg(dto.getMaxPayloadKg());
        if (dto.getActive() != null) vehicle.setActive(dto.getActive());

        log.info("Mise à jour des caractéristiques du véhicule ID : {}", vehicleId);
        return vehicleRepository.save(vehicle);
    }

    /**
     * Active/Désactive un véhicule.
     */
    public void toggleVehicleActive(Long vehicleId, boolean active) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new OptimizationException("Véhicule non trouvé avec l'id : " + vehicleId));
        vehicle.setActive(active);
        vehicleRepository.save(vehicle);
    }

    /**
     * Convertit une entité Vehicle en DTO.
     */
    public VehicleDTO convertToDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setRegistrationNumber(vehicle.getRegistrationNumber());
        dto.setBrand(vehicle.getBrand());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear());
        dto.setMaxVolumeM2(vehicle.getMaxVolumeM2());
        dto.setMaxPayloadKg(vehicle.getMaxPayloadKg());
        dto.setCurrentLoadM2(vehicle.getCurrentLoadM2());
        dto.setCurrentLoadKg(vehicle.getCurrentLoadKg());
        dto.setManagerId(vehicle.getManager().getId());
        dto.setActive(vehicle.getActive());
        return dto;
    }
}

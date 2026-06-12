package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.DriverDTO;
import ma.smartfleet.backend.dto.RegisterRequestDTO;
import ma.smartfleet.backend.dto.UserDTO;
import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.model.Client;
import ma.smartfleet.backend.model.Driver;
import ma.smartfleet.backend.model.Manager;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.model.enums.UserRole;
import ma.smartfleet.backend.repository.DriverRepository;
import ma.smartfleet.backend.repository.ManagerRepository;
import ma.smartfleet.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Enregistre un nouvel utilisateur localement.
     * Instancie la bonne classe d'entité selon le rôle demandé (Client, Driver, Manager).
     */
    public User register(RegisterRequestDTO dto) {
        if (dto.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("La création d'un compte administrateur via l'API est interdite. Les administrateurs doivent être créés directement en base de données.");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà.");
        }

        User user;

        if (dto.getRole() == UserRole.CLIENT) {
            Client client = new Client();
            client.setCompanyName(dto.getCompanyName());
            client.setBusinessAddress(dto.getBusinessAddress());
            client.setBusinessPhone(dto.getBusinessPhone());
            client.setVerified(false);
            user = client;
        } else if (dto.getRole() == UserRole.DRIVER) {
            Driver driver = new Driver();
            driver.setLicenseNumber("TEMP-" + System.currentTimeMillis());
            driver.setLicenseExpiry(null);
            driver.setAvailable(true);
            driver.setManager(null); // Le chauffeur crée son compte de lui-même sans manager initial
            user = driver;
        } else if (dto.getRole() == UserRole.MANAGER) {
            Manager manager = new Manager();
            manager.setDepartment(dto.getDepartment() != null ? dto.getDepartment() : "Opérations");
            manager.setOfficeLocation(dto.getOfficeLocation());
            user = manager;
        } else {
            // Rôle ADMIN ou cas par défaut
            user = new User();
        }

        // Remplir les champs de base de l'utilisateur
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole() != null ? dto.getRole() : UserRole.DRIVER);
        user.setActive(true);

        log.info("Enregistrement réussi pour l'utilisateur local : {} avec le rôle {}", user.getEmail(), user.getRole());
        return userRepository.save(user);
    }



    /**
     * Récupère un utilisateur par son email.
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Récupère un utilisateur par son ID.
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Met à jour les informations de l'utilisateur.
     */
    public User updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new OptimizationException("User not found"));

        if (userDTO.getName() != null) {
            user.setName(userDTO.getName());
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone());
        }
        if (userDTO.getRole() != null) {
            user.setRole(userDTO.getRole());
        }
        if (userDTO.getActive() != null) {
            user.setActive(userDTO.getActive());
        }

        return userRepository.save(user);
    }



    /**
     * Convertit un User en UserDTO.
     */
    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setActive(user.getActive());
        return dto;
    }

    /**
     * Récupère la liste de tous les chauffeurs sans manager (non affectés).
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getUnassignedDrivers() {
        return driverRepository.findByManagerIsNull().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère la liste de tous les chauffeurs d'un Manager donné.
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getDriversByManager(Long managerId) {
        return driverRepository.findByManagerId(managerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertit un Driver en DriverDTO avec ses coordonnées GPS.
     */
    public DriverDTO convertToDriverDTO(Driver driver) {
        DriverDTO dto = new DriverDTO();
        dto.setId(driver.getId());
        dto.setEmail(driver.getEmail());
        dto.setName(driver.getName());
        dto.setPhone(driver.getPhone());
        dto.setActive(driver.getActive());
        dto.setLicenseNumber(driver.getLicenseNumber());
        dto.setLicenseExpiry(driver.getLicenseExpiry());
        dto.setAvailable(driver.getAvailable());
        dto.setManagerId(driver.getManager() != null ? driver.getManager().getId() : null);
        dto.setCurrentLatitude(driver.getCurrentLatitude());
        dto.setCurrentLongitude(driver.getCurrentLongitude());
        dto.setLastLocationUpdate(driver.getLastLocationUpdate());
        return dto;
    }

    /**
     * Récupère les positions GPS actuelles de tous les chauffeurs d'un Manager donné.
     */
    @Transactional(readOnly = true)
    public List<DriverDTO> getDriversLocationsByManager(Long managerId) {
        return driverRepository.findByManagerId(managerId).stream()
                .map(this::convertToDriverDTO)
                .collect(Collectors.toList());
    }

    /**
     * Affecte un chauffeur à l'organisation du Manager connecté.
     */
    public UserDTO assignDriverToManager(Long driverId, Long managerId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new OptimizationException("Chauffeur non trouvé avec l'id : " + driverId));
        
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new OptimizationException("Manager non trouvé avec l'id : " + managerId));

        driver.setManager(manager);
        Driver saved = driverRepository.save(driver);
        log.info("Chauffeur {} affecté au manager {}", driver.getEmail(), manager.getEmail());
        return convertToDTO(saved);
    }

    /**
     * Retire un chauffeur de l'organisation du Manager connecté.
     */
    public UserDTO removeDriverFromManager(Long driverId, Long managerId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new OptimizationException("Chauffeur non trouvé avec l'id : " + driverId));

        if (driver.getManager() == null || !driver.getManager().getId().equals(managerId)) {
            throw new IllegalArgumentException("Ce chauffeur n'est pas affecté à votre organisation.");
        }

        driver.setManager(null);
        Driver saved = driverRepository.save(driver);
        log.info("Chauffeur {} retiré de l'organisation", driver.getEmail());
        return convertToDTO(saved);
    }
}

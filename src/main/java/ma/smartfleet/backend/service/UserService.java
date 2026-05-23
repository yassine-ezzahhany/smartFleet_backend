package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.RegisterRequestDTO;
import ma.smartfleet.backend.dto.UserDTO;
import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.model.Client;
import ma.smartfleet.backend.model.Driver;
import ma.smartfleet.backend.model.Manager;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.model.enums.UserRole;
import ma.smartfleet.backend.repository.ManagerRepository;
import ma.smartfleet.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
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

            // Recherche du manager ou création d'un manager par défaut pour satisfaire la contrainte non-nulle
            Manager manager = null;
            if (manager == null) {
                manager = managerRepository.findAll().stream().findFirst().orElseGet(() -> {
                    log.info("Création d'un manager par défaut pour satisfaire l'association du Driver");
                    Manager defaultManager = new Manager();
                    defaultManager.setEmail("manager.default@smartfleet.ma");
                    defaultManager.setName("Manager Par Défaut");
                    defaultManager.setDepartment("Logistique");
                    defaultManager.setPassword(passwordEncoder.encode("DefaultManagerPass123!"));
                    defaultManager.setRole(UserRole.MANAGER);
                    defaultManager.setActive(true);
                    return managerRepository.save(defaultManager);
                });
            }
            driver.setManager(manager);
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
}

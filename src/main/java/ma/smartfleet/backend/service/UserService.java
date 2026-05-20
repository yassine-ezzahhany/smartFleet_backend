package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.UserDTO;
import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.model.enums.UserRole;
import ma.smartfleet.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * Récupère ou crée un utilisateur basé sur le clerkId.
     * Appelé lors de l'authentification avec Clerk.
     */
    public User getOrCreateUserFromClerk(String clerkId, String email, String firstName, String lastName) {
        return userRepository.findByClerkId(clerkId)
            .orElseGet(() -> {
                log.info("Creating new user with Clerk ID: {}", clerkId);
                User newUser = new User();
                newUser.setClerkId(clerkId);
                newUser.setEmail(email);
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setRole(UserRole.DRIVER); // Rôle par défaut, peut être modifié
                newUser.setActive(true);
                return userRepository.save(newUser);
            });
    }

    /**
     * Récupère un utilisateur par son clerkId.
     */
    public Optional<User> getUserByClerkId(String clerkId) {
        return userRepository.findByClerkId(clerkId);
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

        if (userDTO.getFirstName() != null) {
            user.setFirstName(userDTO.getFirstName());
        }
        if (userDTO.getLastName() != null) {
            user.setLastName(userDTO.getLastName());
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
     * Désactive un utilisateur (appelé lors de la suppression dans Clerk).
     */
    public void deactivateUser(String clerkId) {
        userRepository.findByClerkId(clerkId).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
            log.info("User deactivated: {}", clerkId);
        });
    }

    /**
     * Synchronise les données utilisateur avec Clerk (mise à jour après modification).
     */
    public User syncUserFromClerk(String clerkId, String email, String firstName, String lastName, Boolean active) {
        User user = userRepository.findByClerkId(clerkId)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setClerkId(clerkId);
                newUser.setEmail(email);
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setRole(UserRole.DRIVER);
                newUser.setActive(true);
                return newUser;
            });

        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        if (active != null) {
            user.setActive(active);
        }

        return userRepository.save(user);
    }

    /**
     * Convertit un User en UserDTO.
     */
    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setClerkId(user.getClerkId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setActive(user.getActive());
        return dto;
    }
}

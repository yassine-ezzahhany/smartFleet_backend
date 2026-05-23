package ma.smartfleet.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ma.smartfleet.backend.dto.RegisterRequestDTO;
import ma.smartfleet.backend.dto.UserDTO;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.model.enums.UserRole;
import ma.smartfleet.backend.repository.ManagerRepository;
import ma.smartfleet.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, managerRepository, passwordEncoder);
    }

    @Test
    public void testRegister_Success() {
        // Given
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setEmail("new@example.com");
        dto.setPassword("password123");
        dto.setName("John Doe");
        dto.setRole(UserRole.MANAGER);

        ma.smartfleet.backend.model.Manager savedUser = new ma.smartfleet.backend.model.Manager();
        savedUser.setId(10L);
        savedUser.setEmail(dto.getEmail());
        savedUser.setName(dto.getName());
        savedUser.setRole(UserRole.MANAGER);

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.register(dto);

        // Then
        assertNotNull(result);
        assertEquals(dto.getEmail(), result.getEmail());
        assertEquals(10L, result.getId());
        assertEquals(UserRole.MANAGER, result.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testRegister_AdminForbidden() {
        // Given
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setEmail("admin@example.com");
        dto.setPassword("password123");
        dto.setName("Admin User");
        dto.setRole(UserRole.ADMIN);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.register(dto);
        });

        assertEquals("La création d'un compte administrateur via l'API est interdite. Les administrateurs doivent être créés directement en base de données.", exception.getMessage());
    }


    @Test
    public void testConvertToDTO() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setName("John Doe");
        user.setPhone("+1234567890");
        user.setRole(UserRole.DRIVER);
        user.setActive(true);

        // When
        UserDTO dto = userService.convertToDTO(user);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("user@example.com", dto.getEmail());
        assertEquals("John Doe", dto.getName());
        assertEquals("+1234567890", dto.getPhone());
        assertEquals(UserRole.DRIVER, dto.getRole());
        assertTrue(dto.getActive());
    }
}

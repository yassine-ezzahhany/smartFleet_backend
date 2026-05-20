package ma.smartfleet.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ma.smartfleet.backend.dto.UserDTO;
import ma.smartfleet.backend.model.User;
import ma.smartfleet.backend.model.enums.UserRole;
import ma.smartfleet.backend.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository);
    }

    @Test
    public void testGetOrCreateUserFromClerk_UserExists() {
        // Given
        String clerkId = "user_123456";
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setClerkId(clerkId);
        existingUser.setEmail("test@example.com");

        when(userRepository.findByClerkId(clerkId))
            .thenReturn(Optional.of(existingUser));

        // When
        User result = userService.getOrCreateUserFromClerk(clerkId, "test@example.com", "John", "Doe");

        // Then
        assertNotNull(result);
        assertEquals(clerkId, result.getClerkId());
        assertEquals(1L, result.getId());
    }

    @Test
    public void testGetOrCreateUserFromClerk_UserDoesNotExist() {
        // Given
        String clerkId = "user_new123";
        String email = "newuser@example.com";
        String firstName = "Jane";
        String lastName = "Smith";

        User newUser = new User();
        newUser.setId(2L);
        newUser.setClerkId(clerkId);
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setRole(UserRole.DRIVER);
        newUser.setActive(true);

        when(userRepository.findByClerkId(clerkId))
            .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
            .thenReturn(newUser);

        // When
        User result = userService.getOrCreateUserFromClerk(clerkId, email, firstName, lastName);

        // Then
        assertNotNull(result);
        assertEquals(clerkId, result.getClerkId());
        assertEquals(email, result.getEmail());
        assertEquals(UserRole.DRIVER, result.getRole());
        assertTrue(result.getActive());
    }

    @Test
    public void testSyncUserFromClerk() {
        // Given
        String clerkId = "user_sync123";
        String email = "sync@example.com";
        
        User existingUser = new User();
        existingUser.setId(3L);
        existingUser.setClerkId(clerkId);
        existingUser.setEmail("oldemail@example.com");
        existingUser.setFirstName("Old");
        existingUser.setLastName("Name");

        when(userRepository.findByClerkId(clerkId))
            .thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class)))
            .thenReturn(existingUser);

        // When
        User result = userService.syncUserFromClerk(clerkId, email, "New", "Name", true);

        // Then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
        assertTrue(result.getActive());
    }

    @Test
    public void testConvertToDTO() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setClerkId("user_123");
        user.setEmail("user@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhone("+1234567890");
        user.setRole(UserRole.DRIVER);
        user.setActive(true);

        // When
        UserDTO dto = userService.convertToDTO(user);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("user_123", dto.getClerkId());
        assertEquals("user@example.com", dto.getEmail());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("+1234567890", dto.getPhone());
        assertEquals(UserRole.DRIVER, dto.getRole());
        assertTrue(dto.getActive());
    }
}

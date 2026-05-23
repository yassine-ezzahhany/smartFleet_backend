package ma.smartfleet.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.smartfleet.backend.model.enums.UserRole;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestDTO {
    // Info utilisateur de base
    private String email;
    private String password;
    private String name;
    private String phone;
    private UserRole role;

    // Champs spécifiques pour CLIENT
    private String companyName;
    private String businessAddress;
    private String businessPhone;

    // Champs spécifiques pour MANAGER
    private String department;
    private String officeLocation;
}

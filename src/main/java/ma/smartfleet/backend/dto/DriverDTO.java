package ma.smartfleet.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverDTO {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String licenseNumber;
    private String licenseExpiry;
    private Boolean available;
    private Boolean active;
    private Long managerId;
}

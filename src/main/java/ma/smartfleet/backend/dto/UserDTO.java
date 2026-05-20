package ma.smartfleet.backend.dto;

import lombok.*;
import ma.smartfleet.backend.model.enums.UserRole;

@Data @AllArgsConstructor @NoArgsConstructor
public class UserDTO {
    private Long id;
    private String clerkId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private UserRole role;
    private Boolean active;
}

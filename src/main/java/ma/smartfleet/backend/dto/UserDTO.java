package ma.smartfleet.backend.dto;

import lombok.*;
import ma.smartfleet.backend.model.enums.UserRole;

@Data @AllArgsConstructor @NoArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private UserRole role;
    private Boolean active;
}

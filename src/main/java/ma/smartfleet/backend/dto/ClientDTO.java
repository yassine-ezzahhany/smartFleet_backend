package ma.smartfleet.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientDTO {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String companyName;
    private String businessAddress;
    private String businessPhone;
}

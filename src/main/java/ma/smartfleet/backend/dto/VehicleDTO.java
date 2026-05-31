package ma.smartfleet.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDTO {
    private Long id;
    private String registrationNumber;
    private String brand;
    private String model;
    private Integer year;
    private Double maxVolumeM2;
    private Double maxPayloadKg;
    private Double currentLoadM2;
    private Double currentLoadKg;
    private Long managerId;
    private Boolean active;
}

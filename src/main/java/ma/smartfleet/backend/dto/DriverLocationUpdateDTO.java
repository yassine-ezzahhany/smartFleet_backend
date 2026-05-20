package ma.smartfleet.backend.dto;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor
public class DriverLocationUpdateDTO {
    private Long driverId;
    private Double latitude;
    private Double longitude;
    private Long timestamp;
}

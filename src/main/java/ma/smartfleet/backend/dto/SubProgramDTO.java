package ma.smartfleet.backend.dto;

import lombok.*;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class SubProgramDTO {
    private Long id;
    private String subProgramNumber;
    private Long deliveryProgramId;
    private Long driverId;
    private Long vehicleId;
    private List<Long> orderIds;
    private String status;
    private String polyline;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private Double actualDistanceKm;
    private Integer actualDurationMinutes;
    private String startTime;
    private String endTime;
    private Integer totalOrdersCount;
    private Integer approvedOrdersCount;
}

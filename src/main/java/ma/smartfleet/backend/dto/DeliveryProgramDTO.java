package ma.smartfleet.backend.dto;

import lombok.*;

import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor
public class DeliveryProgramDTO {
    private Long id;
    private String programNumber;
    private Long managerId;
    private String status;
    private List<OrderDTO> orders;
    private List<SubProgramDTO> subPrograms;
    private String plannedDate;
    private String executionDate;
    private String completionDate;
    private String notes;
}

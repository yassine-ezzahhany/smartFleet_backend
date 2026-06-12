package ma.smartfleet.backend.dto;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long clientId;
    private Double weightKg;
    private Double volumeM2;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryAddress;
    private String deliveryDescription;
    private String status;
    private String estimatedDeliveryTime;
    private String actualDeliveryTime;
    private Boolean clientApproved;
    private String priority;
    private Integer visitSequence;
}

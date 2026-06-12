package ma.smartfleet.backend.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;
import ma.smartfleet.backend.model.enums.OrderStatus;
import ma.smartfleet.backend.model.enums.OrderPriority;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number", unique = true),
    @Index(name = "idx_order_client", columnList = "client_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_delivery_program", columnList = "delivery_program_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Client client;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "volume_m2", nullable = false)
    private Double volumeM2;

    @Column(name = "delivery_latitude", nullable = false)
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude", nullable = false)
    private Double deliveryLongitude;

    @Column(columnDefinition = "geography(POINT,4326)")
    private Point deliveryLocation;

    @Column
    private String deliveryAddress;

    @Column
    private String deliveryDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderPriority priority = OrderPriority.NORMAL;

    @Column(name = "visit_sequence")
    private Integer visitSequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_program_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private DeliveryProgram deliveryProgram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_program_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private SubProgram subProgram;

    @Column
    private LocalDateTime estimatedDeliveryTime;

    @Column
    private LocalDateTime actualDeliveryTime;

    @Column(nullable = false)
    private Boolean clientApproved = false;

    @Column
    private LocalDateTime clientApprovalTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

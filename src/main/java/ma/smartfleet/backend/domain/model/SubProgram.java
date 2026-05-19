package ma.smartfleet.backend.domain.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ma.smartfleet.backend.domain.model.enums.SubProgramStatus;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sub_programs", indexes = {
    @Index(name = "idx_sub_program_number", columnList = "sub_program_number", unique = true),
    @Index(name = "idx_sub_program_driver", columnList = "driver_id"),
    @Index(name = "idx_sub_program_vehicle", columnList = "vehicle_id"),
    @Index(name = "idx_sub_program_delivery_program", columnList = "delivery_program_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubProgram {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sub_program_number", unique = true, nullable = false)
    private String subProgramNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_program_id", nullable = false)
    private DeliveryProgram deliveryProgram;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    
    @OneToMany(mappedBy = "subProgram", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Order> orders = new HashSet<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubProgramStatus status = SubProgramStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String polyline;
    
    @Column(name = "estimated_distance_km")
    private Double estimatedDistanceKm;
    
    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;
    
    @Column(name = "actual_distance_km")
    private Double actualDistanceKm;
    
    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;
    
    @Column
    private LocalDateTime startTime;
    
    @Column
    private LocalDateTime endTime;
    
    @Column
    private Integer totalOrdersCount = 0;
    
    @Column
    private Integer approvedOrdersCount = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

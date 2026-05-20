package ma.smartfleet.backend.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vehicles", indexes = {
    @Index(name = "idx_vehicle_registration", columnList = "registration_number", unique = true),
    @Index(name = "idx_vehicle_manager", columnList = "manager_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", unique = true, nullable = false)
    private String registrationNumber;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "max_volume_m2", nullable = false)
    private Double maxVolumeM2;

    @Column(name = "max_payload_kg", nullable = false)
    private Double maxPayloadKg;

    @Column(nullable = false)
    private Double currentLoadM2 = 0.0;

    @Column(nullable = false)
    private Double currentLoadKg = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @ManyToMany(mappedBy = "assignedVehicles")
    private Set<Driver> assignedDrivers = new HashSet<>();

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

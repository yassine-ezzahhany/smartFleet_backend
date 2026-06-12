package ma.smartfleet.backend.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ma.smartfleet.backend.model.enums.DeliveryProgramStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "delivery_programs", indexes = {
    @Index(name = "idx_delivery_program_number", columnList = "program_number", unique = true),
    @Index(name = "idx_delivery_program_manager", columnList = "manager_id"),
    @Index(name = "idx_delivery_program_status", columnList = "status")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_number", unique = true, nullable = false)
    private String programNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Manager manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryProgramStatus status = DeliveryProgramStatus.PENDING;

    @OneToMany(mappedBy = "deliveryProgram", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Order> orders = new HashSet<>();

    @OneToMany(mappedBy = "deliveryProgram", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<SubProgram> subPrograms = new HashSet<>();

    @Column
    private LocalDateTime plannedDate;

    @Column
    private LocalDateTime executionDate;

    @Column
    private LocalDateTime completionDate;

    @Column
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

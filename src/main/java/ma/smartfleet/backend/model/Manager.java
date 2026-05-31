package ma.smartfleet.backend.model;

import lombok.*;
import jakarta.persistence.*;
import ma.smartfleet.backend.model.enums.UserRole;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "managers", indexes = {
    @Index(name = "idx_manager_department", columnList = "department")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Manager extends User {

    @Column(nullable = false)
    private String department;

    @Column
    private String officeLocation;

    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Vehicle> managedVehicles = new HashSet<>();

    @PrePersist
    private void setRole() {
        this.setRole(UserRole.MANAGER);
    }
}

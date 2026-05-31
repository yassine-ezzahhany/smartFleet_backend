package ma.smartfleet.backend.model;

import lombok.*;
import jakarta.persistence.*;
import ma.smartfleet.backend.model.enums.UserRole;
import org.locationtech.jts.geom.Point;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "drivers", indexes = {
    @Index(name = "idx_driver_license", columnList = "license_number", unique = true),
    @Index(name = "idx_driver_manager", columnList = "manager_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Driver extends User {

    @Column(name = "license_number", unique = true, nullable = false)
    private String licenseNumber;

    @Column
    private String licenseExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = true)
    private Manager manager;

    @ManyToMany
    @JoinTable(
        name = "driver_vehicles",
        joinColumns = @JoinColumn(name = "driver_id"),
        inverseJoinColumns = @JoinColumn(name = "vehicle_id")
    )
    private Set<Vehicle> assignedVehicles = new HashSet<>();

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(columnDefinition = "geography(POINT,4326)")
    private Point currentLocation;

    @Column(name = "last_location_update")
    private Long lastLocationUpdate;

    @Column(nullable = false)
    private Boolean available = true;

    @Column
    private String firebaseDeviceToken;

    @PrePersist
    private void setRole() {
        this.setRole(UserRole.DRIVER);
    }
}

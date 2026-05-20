package ma.smartfleet.backend.repository;

import ma.smartfleet.backend.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
    List<Vehicle> findByManagerId(Long managerId);
    List<Vehicle> findByManagerIdAndActiveTrue(Long managerId);
}

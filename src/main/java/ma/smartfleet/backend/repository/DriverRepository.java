package ma.smartfleet.backend.repository;

import ma.smartfleet.backend.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByClerkId(String clerkId);
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    List<Driver> findByManagerId(Long managerId);
    List<Driver> findByManagerIdAndAvailableTrue(Long managerId);

    @Query(value = "SELECT * FROM drivers d WHERE ST_DWithin(d.current_location, ST_SetSRID(ST_MakePoint(?1, ?2), 4326)::geography, ?3)", nativeQuery = true)
    List<Driver> findDriversNearby(Double longitude, Double latitude, Double radiusMeters);
}

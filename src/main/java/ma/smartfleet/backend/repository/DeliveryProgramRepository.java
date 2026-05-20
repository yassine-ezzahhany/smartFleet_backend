package ma.smartfleet.backend.repository;

import ma.smartfleet.backend.model.DeliveryProgram;
import ma.smartfleet.backend.model.enums.DeliveryProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryProgramRepository extends JpaRepository<DeliveryProgram, Long> {
    Optional<DeliveryProgram> findByProgramNumber(String programNumber);
    List<DeliveryProgram> findByManagerId(Long managerId);
    List<DeliveryProgram> findByManagerIdAndStatus(Long managerId, DeliveryProgramStatus status);
    List<DeliveryProgram> findByStatus(DeliveryProgramStatus status);
}

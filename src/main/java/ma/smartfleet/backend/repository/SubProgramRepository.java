package ma.smartfleet.backend.repository;

import ma.smartfleet.backend.model.SubProgram;
import ma.smartfleet.backend.model.enums.SubProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubProgramRepository extends JpaRepository<SubProgram, Long> {
    List<SubProgram> findByDeliveryProgramId(Long deliveryProgramId);
    List<SubProgram> findByDriverId(Long driverId);
    List<SubProgram> findByDriverIdAndStatus(Long driverId, SubProgramStatus status);
}

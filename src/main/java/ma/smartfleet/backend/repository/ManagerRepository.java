package ma.smartfleet.backend.repository;

import ma.smartfleet.backend.model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Optional<Manager> findByClerkId(String clerkId);
    Optional<Manager> findByEmail(String email);
}

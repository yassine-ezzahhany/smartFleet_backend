package ma.smartfleet.backend.repository;

import ma.smartfleet.backend.model.Order;
import ma.smartfleet.backend.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByClientId(Long clientId);
    List<Order> findByDeliveryProgramId(Long deliveryProgramId);
    List<Order> findByDeliveryProgramIdAndStatus(Long deliveryProgramId, OrderStatus status);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findBySubProgramId(Long subProgramId);

    @Query(value = "SELECT * FROM orders o WHERE ST_DWithin(o.delivery_location, ST_SetSRID(ST_MakePoint(?1, ?2), 4326)::geography, ?3)", nativeQuery = true)
    List<Order> findOrdersNearby(Double longitude, Double latitude, Double radiusMeters);
}

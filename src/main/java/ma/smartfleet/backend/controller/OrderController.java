package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.dto.OrderDTO;
import ma.smartfleet.backend.model.Order;
import ma.smartfleet.backend.model.enums.OrderStatus;
import ma.smartfleet.backend.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(toDto(order));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<OrderDTO> approveDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(orderService.approveDelivery(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OrderDTO> rejectOrder(@PathVariable Long id,
                                                @RequestBody String reason) {
        return ResponseEntity.ok(toDto(orderService.rejectOrder(id, reason)));
    }

    private OrderDTO toDto(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setClientId(order.getClient() != null ? order.getClient().getId() : null);
        dto.setWeightKg(order.getWeightKg());
        dto.setVolumeM2(order.getVolumeM2());
        dto.setDeliveryLatitude(order.getDeliveryLatitude());
        dto.setDeliveryLongitude(order.getDeliveryLongitude());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setDeliveryDescription(order.getDeliveryDescription());
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        dto.setClientApproved(order.getClientApproved());
        dto.setEstimatedDeliveryTime(order.getEstimatedDeliveryTime() != null ? order.getEstimatedDeliveryTime().toString() : null);
        dto.setActualDeliveryTime(order.getActualDeliveryTime() != null ? order.getActualDeliveryTime().toString() : null);
        return dto;
    }
}

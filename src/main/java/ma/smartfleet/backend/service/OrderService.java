package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.exception.ResourceNotFoundException;
import ma.smartfleet.backend.model.Order;
import ma.smartfleet.backend.model.SubProgram;
import ma.smartfleet.backend.model.enums.OrderStatus;
import ma.smartfleet.backend.model.enums.SubProgramStatus;
import ma.smartfleet.backend.repository.OrderRepository;
import ma.smartfleet.backend.repository.SubProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final SubProgramRepository subProgramRepository;

    @Transactional
    public Order approveDelivery(Long orderId) {
        log.info("Approving delivery for order id: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        order.setClientApproved(true);
        order.setClientApprovalTime(LocalDateTime.now());
        order.setStatus(OrderStatus.DELIVERED);
        order.setActualDeliveryTime(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        if (order.getSubProgram() != null) {
            checkAndCompleteSubProgram(order.getSubProgram().getId());
        }
        return saved;
    }

    @Transactional
    public Order rejectOrder(Long orderId, String reason) {
        log.info("Rejecting order id: {} reason: {}", orderId, reason);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setStatus(OrderStatus.REJECTED);
        order.setDeliveryDescription(reason);
        return orderRepository.save(order);
    }

    @Transactional
    public boolean checkAndCompleteSubProgram(Long subProgramId) {
        SubProgram subProgram = subProgramRepository.findById(subProgramId)
                .orElseThrow(() -> new ResourceNotFoundException("SubProgram", subProgramId));

        Set<Order> orders = subProgram.getOrders();
        if (orders == null || orders.isEmpty()) return false;

        int approvedCount = 0;
        boolean allDone = true;

        for (Order o : orders) {
            if (Boolean.TRUE.equals(o.getClientApproved())) {
                approvedCount++;
            } else if (o.getStatus() != OrderStatus.REJECTED && o.getStatus() != OrderStatus.CANCELLED) {
                allDone = false;
            }
        }

        subProgram.setApprovedOrdersCount(approvedCount);

        if (allDone) {
            subProgram.setStatus(SubProgramStatus.COMPLETED);
            subProgram.setEndTime(LocalDateTime.now());
            log.info("SubProgram {} completed", subProgramId);
        }

        subProgramRepository.save(subProgram);
        return allDone;
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}

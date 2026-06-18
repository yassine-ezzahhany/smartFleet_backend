package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.dto.DeliveryProgramDTO;
import ma.smartfleet.backend.dto.OrderDTO;
import ma.smartfleet.backend.dto.SubProgramDTO;
import ma.smartfleet.backend.exception.ResourceNotFoundException;
import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.model.*;
import ma.smartfleet.backend.model.enums.DeliveryProgramStatus;
import ma.smartfleet.backend.model.enums.OrderStatus;
import ma.smartfleet.backend.model.enums.SubProgramStatus;
import ma.smartfleet.backend.repository.DeliveryProgramRepository;
import ma.smartfleet.backend.repository.ManagerRepository;
import ma.smartfleet.backend.repository.OrderRepository;
import ma.smartfleet.backend.repository.SubProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryProgramService {

    private final DeliveryProgramRepository deliveryProgramRepository;
    private final ManagerRepository managerRepository;
    private final OrderRepository orderRepository;
    private final SubProgramRepository subProgramRepository;

    public DeliveryProgramDTO createProgram(DeliveryProgramDTO dto, Long managerId) {
        log.info("Creating delivery program for manager id: {}", managerId);
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        DeliveryProgram program = new DeliveryProgram();
        if (dto.getProgramNumber() != null && !dto.getProgramNumber().trim().isEmpty()) {
            program.setProgramNumber(dto.getProgramNumber());
        } else {
            program.setProgramNumber("PRG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        program.setManager(manager);
        program.setStatus(DeliveryProgramStatus.PENDING);
        program.setNotes(dto.getNotes());
        
        if (dto.getPlannedDate() != null && !dto.getPlannedDate().trim().isEmpty()) {
            program.setPlannedDate(LocalDateTime.parse(dto.getPlannedDate()));
        }

        DeliveryProgram saved = deliveryProgramRepository.save(program);

        if (dto.getOrders() != null && !dto.getOrders().isEmpty()) {
            for (OrderDTO orderDTO : dto.getOrders()) {
                if (orderDTO.getId() != null) {
                    Order order = orderRepository.findById(orderDTO.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Order", orderDTO.getId()));
                    if (order.getDeliveryProgram() != null && order.getDeliveryProgram().getStatus() != DeliveryProgramStatus.CANCELLED) {
                        throw new IllegalArgumentException("Order " + order.getId() + " is already assigned to active program: " + order.getDeliveryProgram().getProgramNumber());
                    }
                    order.setDeliveryProgram(saved);
                    orderRepository.save(order);
                    saved.getOrders().add(order);
                }
            }
            saved = deliveryProgramRepository.save(saved);
        }

        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<DeliveryProgramDTO> getProgramsByManager(Long managerId, String statusStr) {
        log.info("Getting programs for manager id: {}, status: {}", managerId, statusStr);
        List<DeliveryProgram> programs;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                DeliveryProgramStatus status = DeliveryProgramStatus.valueOf(statusStr.toUpperCase());
                programs = deliveryProgramRepository.findByManagerIdAndStatus(managerId, status);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + statusStr);
            }
        } else {
            programs = deliveryProgramRepository.findByManagerId(managerId);
        }
        return programs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryProgramDTO getProgramById(Long id, Long managerId) {
        log.info("Getting program id: {} for manager: {}", id, managerId);
        DeliveryProgram program = deliveryProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryProgram", id));
        if (!program.getManager().getId().equals(managerId)) {
            throw new OptimizationException("You do not have permission to access this delivery program.");
        }
        return convertToDTO(program);
    }

    public DeliveryProgramDTO updateProgram(Long id, DeliveryProgramDTO dto, Long managerId) {
        log.info("Updating program id: {} for manager: {}", id, managerId);
        DeliveryProgram program = deliveryProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryProgram", id));
        if (!program.getManager().getId().equals(managerId)) {
            throw new OptimizationException("You do not have permission to update this delivery program.");
        }

        if (dto.getNotes() != null) {
            program.setNotes(dto.getNotes());
        }
        if (dto.getPlannedDate() != null && !dto.getPlannedDate().trim().isEmpty()) {
            program.setPlannedDate(LocalDateTime.parse(dto.getPlannedDate()));
        }
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            try {
                DeliveryProgramStatus newStatus = DeliveryProgramStatus.valueOf(dto.getStatus().toUpperCase());
                if (newStatus == DeliveryProgramStatus.CANCELLED && program.getStatus() != DeliveryProgramStatus.CANCELLED) {
                    cancelProgram(program);
                } else {
                    program.setStatus(newStatus);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + dto.getStatus());
            }
        }

        DeliveryProgram saved = deliveryProgramRepository.save(program);
        return convertToDTO(saved);
    }

    public void deleteProgram(Long id, Long managerId) {
        log.info("Deleting program id: {} for manager: {}", id, managerId);
        DeliveryProgram program = deliveryProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryProgram", id));
        if (!program.getManager().getId().equals(managerId)) {
            throw new OptimizationException("You do not have permission to delete this delivery program.");
        }
        if (program.getStatus() != DeliveryProgramStatus.PENDING) {
            throw new IllegalStateException("Cannot delete a delivery program that is not in PENDING state.");
        }

        // Dissocier les commandes de façon sécurisée
        for (Order order : new ArrayList<>(program.getOrders())) {
            order.setDeliveryProgram(null);
            order.setSubProgram(null);
            if (order.getStatus() == OrderStatus.ASSIGNED) {
                order.setStatus(OrderStatus.PENDING);
            }
            orderRepository.save(order);
        }
        program.getOrders().clear();

        deliveryProgramRepository.delete(program);
    }

    public DeliveryProgramDTO addOrdersToProgram(Long id, List<Long> orderIds, Long managerId) {
        log.info("Adding orders {} to program id: {}", orderIds, id);
        DeliveryProgram program = deliveryProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryProgram", id));
        if (!program.getManager().getId().equals(managerId)) {
            throw new OptimizationException("You do not have permission to modify this delivery program.");
        }

        for (Long orderId : orderIds) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
            if (order.getDeliveryProgram() != null && !order.getDeliveryProgram().getId().equals(program.getId()) 
                    && order.getDeliveryProgram().getStatus() != DeliveryProgramStatus.CANCELLED) {
                throw new IllegalArgumentException("Order " + orderId + " is already assigned to active program: " + order.getDeliveryProgram().getProgramNumber());
            }
            order.setDeliveryProgram(program);
            orderRepository.save(order);
            program.getOrders().add(order);
        }

        DeliveryProgram saved = deliveryProgramRepository.save(program);
        return convertToDTO(saved);
    }

    public DeliveryProgramDTO removeOrderFromProgram(Long id, Long orderId, Long managerId) {
        log.info("Removing order {} from program id: {}", orderId, id);
        DeliveryProgram program = deliveryProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryProgram", id));
        if (!program.getManager().getId().equals(managerId)) {
            throw new OptimizationException("You do not have permission to modify this delivery program.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getDeliveryProgram() == null || !order.getDeliveryProgram().getId().equals(id)) {
            throw new IllegalArgumentException("Order " + orderId + " does not belong to program " + id);
        }

        order.setDeliveryProgram(null);
        order.setSubProgram(null);
        if (order.getStatus() == OrderStatus.ASSIGNED) {
            order.setStatus(OrderStatus.PENDING);
        }
        orderRepository.save(order);
        program.getOrders().remove(order);

        DeliveryProgram saved = deliveryProgramRepository.save(program);
        return convertToDTO(saved);
    }

    private void cancelProgram(DeliveryProgram program) {
        program.setStatus(DeliveryProgramStatus.CANCELLED);
        for (Order order : program.getOrders()) {
            order.setSubProgram(null);
            if (order.getStatus() == OrderStatus.ASSIGNED) {
                order.setStatus(OrderStatus.PENDING);
            }
            orderRepository.save(order);
        }
        for (SubProgram sp : program.getSubPrograms()) {
            sp.setStatus(SubProgramStatus.CANCELLED);
            subProgramRepository.save(sp);
        }
    }

    public DeliveryProgramDTO convertToDTO(DeliveryProgram program) {
        DeliveryProgramDTO dto = new DeliveryProgramDTO();
        dto.setId(program.getId());
        dto.setProgramNumber(program.getProgramNumber());
        dto.setManagerId(program.getManager() != null ? program.getManager().getId() : null);
        dto.setStatus(program.getStatus() != null ? program.getStatus().name() : null);
        dto.setPlannedDate(program.getPlannedDate() != null ? program.getPlannedDate().toString() : null);
        dto.setExecutionDate(program.getExecutionDate() != null ? program.getExecutionDate().toString() : null);
        dto.setCompletionDate(program.getCompletionDate() != null ? program.getCompletionDate().toString() : null);
        dto.setNotes(program.getNotes());
        
        if (program.getOrders() != null) {
            dto.setOrders(program.getOrders().stream()
                    .map(this::convertOrderToDTO)
                    .collect(Collectors.toList()));
        }
        
        if (program.getSubPrograms() != null) {
            dto.setSubPrograms(program.getSubPrograms().stream()
                    .map(this::convertSubProgramToDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    private OrderDTO convertOrderToDTO(Order order) {
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
        dto.setPriority(order.getPriority() != null ? order.getPriority().name() : null);
        dto.setVisitSequence(order.getVisitSequence());
        return dto;
    }

    private SubProgramDTO convertSubProgramToDTO(SubProgram sp) {
        SubProgramDTO dto = new SubProgramDTO();
        dto.setId(sp.getId());
        dto.setSubProgramNumber(sp.getSubProgramNumber());
        dto.setDeliveryProgramId(sp.getDeliveryProgram() != null ? sp.getDeliveryProgram().getId() : null);
        dto.setDriverId(sp.getDriver() != null ? sp.getDriver().getId() : null);
        dto.setVehicleId(sp.getVehicle() != null ? sp.getVehicle().getId() : null);
        dto.setStatus(sp.getStatus() != null ? sp.getStatus().name() : null);
        dto.setPolyline(sp.getPolyline());
        dto.setEstimatedDistanceKm(sp.getEstimatedDistanceKm());
        dto.setEstimatedDurationMinutes(sp.getEstimatedDurationMinutes());
        dto.setActualDistanceKm(sp.getActualDistanceKm());
        dto.setActualDurationMinutes(sp.getActualDurationMinutes());
        dto.setTotalOrdersCount(sp.getTotalOrdersCount());
        dto.setApprovedOrdersCount(sp.getApprovedOrdersCount());
        dto.setStartTime(sp.getStartTime() != null ? sp.getStartTime().toString() : null);
        dto.setEndTime(sp.getEndTime() != null ? sp.getEndTime().toString() : null);
        if (sp.getOrders() != null) {
            dto.setOrderIds(sp.getOrders().stream().map(Order::getId).collect(Collectors.toList()));
        }
        return dto;
    }
}

package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.exception.ResourceNotFoundException;
import ma.smartfleet.backend.model.*;
import ma.smartfleet.backend.model.enums.DeliveryProgramStatus;
import ma.smartfleet.backend.model.enums.SubProgramStatus;
import ma.smartfleet.backend.repository.*;
import ma.smartfleet.backend.infrastructure.adapter.ORToolsAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryOptimizationService {

    private final DeliveryProgramRepository deliveryProgramRepository;
    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final SubProgramRepository subProgramRepository;
    private final ORToolsAdapter orToolsAdapter;

    @Transactional
    public DeliveryProgram optimizeProgram(Long programId) {
        log.info("Optimizing delivery program id={}", programId);
        DeliveryProgram program = deliveryProgramRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryProgram", programId));

        List<Order> orders = new ArrayList<>(program.getOrders());
        if (orders.isEmpty()) {
            throw new OptimizationException("No orders to optimize in program " + program.getProgramNumber());
        }

        List<Driver> drivers = driverRepository.findByManagerIdAndAvailableTrue(program.getManager().getId());
        List<Vehicle> vehicles = vehicleRepository.findByManagerIdAndActiveTrue(program.getManager().getId());

        if (drivers.isEmpty()) {
            throw new OptimizationException("No available drivers for manager " + program.getManager().getId());
        }
        if (vehicles.isEmpty()) {
            throw new OptimizationException("No active vehicles for manager " + program.getManager().getId());
        }

        // 1. Appel du solveur d'optimisation OR-Tools VRP
        ORToolsAdapter.OptimizationResult optimizationResult = orToolsAdapter.optimizeDeliveries(orders, vehicles, drivers);

        Set<SubProgram> subPrograms = new HashSet<>();
        int routeCounter = 1;

        // 2. Mappage des tournées optimisées vers les SubPrograms
        for (ORToolsAdapter.RouteSolution route : optimizationResult.getRoutes()) {
            Vehicle vehicle = vehicles.stream()
                    .filter(v -> v.getId().equals(route.getVehicleId()))
                    .findFirst()
                    .orElse(vehicles.get(0));

            Driver driver = drivers.stream()
                    .filter(d -> d.getId().equals(route.getDriverId()))
                    .findFirst()
                    .orElse(drivers.get(0));

            SubProgram sp = new SubProgram();
            sp.setSubProgramNumber(program.getProgramNumber() + "-SUB-" + routeCounter++);
            sp.setDeliveryProgram(program);
            sp.setDriver(driver);
            sp.setVehicle(vehicle);
            sp.setStatus(SubProgramStatus.ASSIGNED);
            sp.setEstimatedDistanceKm(route.getRouteDistance() / 1000.0);
            sp.setEstimatedDurationMinutes((int) Math.round(route.getRouteTime() / 60.0));
            sp.setOrders(new HashSet<>());
            sp.setTotalOrdersCount(route.getOrderIds().size());

            SubProgram savedSp = subProgramRepository.save(sp);

            // Associer les commandes affectées à cette tournée
            for (Long orderId : route.getOrderIds()) {
                Order order = orders.stream()
                        .filter(o -> o.getId().equals(orderId))
                        .findFirst()
                        .orElse(null);
                if (order != null) {
                    order.setSubProgram(savedSp);
                    order.setStatus(ma.smartfleet.backend.model.enums.OrderStatus.ASSIGNED);
                    orderRepository.save(order);
                    savedSp.getOrders().add(order);
                }
            }

            subPrograms.add(savedSp);
        }

        program.setSubPrograms(subPrograms);
        program.setStatus(DeliveryProgramStatus.OPTIMIZED);
        return deliveryProgramRepository.save(program);
    }

    @Transactional(readOnly = true)
    public OptimizationStats getStats(Long programId) {
        DeliveryProgram program = deliveryProgramRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryProgram", programId));

        int totalOrders = program.getOrders().size();
        int totalSub    = program.getSubPrograms().size();
        double totalKm  = program.getSubPrograms().stream()
                .mapToDouble(sp -> sp.getEstimatedDistanceKm() != null ? sp.getEstimatedDistanceKm() : 0.0)
                .sum();
        int totalMin    = program.getSubPrograms().stream()
                .mapToInt(sp -> sp.getEstimatedDurationMinutes() != null ? sp.getEstimatedDurationMinutes() : 0)
                .sum();
        long vehicles   = program.getSubPrograms().stream()
                .map(sp -> sp.getVehicle().getId()).distinct().count();

        return new OptimizationStats(programId, totalOrders, totalSub, (int) vehicles, totalKm, totalMin, LocalDateTime.now().toString());
    }

    private SubProgram newSubProgram(DeliveryProgram program, Driver driver, Vehicle vehicle, int counter) {
        SubProgram sp = new SubProgram();
        sp.setSubProgramNumber(program.getProgramNumber() + "-SUB-" + counter);
        sp.setDeliveryProgram(program);
        sp.setDriver(driver);
        sp.setVehicle(vehicle);
        sp.setStatus(SubProgramStatus.ASSIGNED);
        sp.setOrders(new HashSet<>());
        return subProgramRepository.save(sp);
    }

    public record OptimizationStats(
            Long deliveryProgramId,
            int totalOrders,
            int totalSubPrograms,
            int totalVehicles,
            double totalDistanceKm,
            int totalDurationMinutes,
            String optimizationDate) {}
}

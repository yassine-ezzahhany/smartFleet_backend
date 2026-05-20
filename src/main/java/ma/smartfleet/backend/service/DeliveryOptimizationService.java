package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.exception.ResourceNotFoundException;
import ma.smartfleet.backend.model.*;
import ma.smartfleet.backend.model.enums.DeliveryProgramStatus;
import ma.smartfleet.backend.model.enums.SubProgramStatus;
import ma.smartfleet.backend.repository.*;
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

        // Simple round-robin assignment as a placeholder for full OR-Tools VRP.
        // When OR-Tools is integrated, replace this block with the solver call.
        Set<SubProgram> subPrograms = new HashSet<>();
        int driverIdx = 0, vehicleIdx = 0, routeCounter = 1;

        SubProgram current = newSubProgram(program, drivers.get(driverIdx), vehicles.get(vehicleIdx), routeCounter++);
        double currentWeight = 0, currentVolume = 0;

        for (Order order : orders) {
            double maxKg   = current.getVehicle().getMaxPayloadKg();
            double maxM2   = current.getVehicle().getMaxVolumeM2();

            // If adding this order exceeds capacity, open a new sub-program
            if (currentWeight + order.getWeightKg() > maxKg || currentVolume + order.getVolumeM2() > maxM2) {
                subPrograms.add(current);

                driverIdx  = (driverIdx  + 1) % drivers.size();
                vehicleIdx = (vehicleIdx + 1) % vehicles.size();
                current     = newSubProgram(program, drivers.get(driverIdx), vehicles.get(vehicleIdx), routeCounter++);
                currentWeight = 0;
                currentVolume = 0;
            }

            order.setSubProgram(current);
            current.getOrders().add(order);
            currentWeight += order.getWeightKg();
            currentVolume += order.getVolumeM2();
        }
        subPrograms.add(current);

        // Update counts
        for (SubProgram sp : subPrograms) {
            sp.setTotalOrdersCount(sp.getOrders().size());
            subProgramRepository.save(sp);
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

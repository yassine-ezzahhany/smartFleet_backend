package ma.smartfleet.backend.infrastructure.adapter;

import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.model.Driver;
import ma.smartfleet.backend.model.Order;
import ma.smartfleet.backend.model.Vehicle;
import ma.smartfleet.backend.model.enums.OrderPriority;
import ma.smartfleet.backend.service.ValhallaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ORToolsAdapterTest {

    private ORToolsAdapter orToolsAdapter;

    @BeforeEach
    public void setUp() {
        orToolsAdapter = new ORToolsAdapter();
        ReflectionTestUtils.setField(orToolsAdapter, "depotLat", 33.9716);
        ReflectionTestUtils.setField(orToolsAdapter, "depotLon", -6.8498);
        ReflectionTestUtils.setField(orToolsAdapter, "timeLimitSeconds", 5);
    }

    private ValhallaClient.MatrixResult createDummyMatrix(int size) {
        double[][] distances = new double[size][size];
        double[][] durations = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    distances[i][j] = 0.0;
                    durations[i][j] = 0.0;
                } else {
                    distances[i][j] = 1000.0; // 1km
                    durations[i][j] = 120.0;  // 2 minutes
                }
            }
        }
        return new ValhallaClient.MatrixResult(distances, durations);
    }

    @Test
    public void testOptimizeDeliveries_Success() {
        // Given
        List<Order> orders = new ArrayList<>();
        
        Order order1 = new Order();
        order1.setId(101L);
        order1.setOrderNumber("ORD-001");
        order1.setWeightKg(100.0);
        order1.setVolumeM2(0.5);
        order1.setDeliveryLatitude(33.9800);
        order1.setDeliveryLongitude(-6.8400);
        order1.setPriority(OrderPriority.NORMAL);
        orders.add(order1);

        Order order2 = new Order();
        order2.setId(102L);
        order2.setOrderNumber("ORD-002");
        order2.setWeightKg(150.0);
        order2.setVolumeM2(0.8);
        order2.setDeliveryLatitude(33.9600);
        order2.setDeliveryLongitude(-6.8600);
        order2.setPriority(OrderPriority.NORMAL);
        orders.add(order2);

        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setId(10L);
        vehicle1.setRegistrationNumber("VEH-001");
        vehicle1.setMaxPayloadKg(500.0);
        vehicle1.setMaxVolumeM2(2.0);
        vehicles.add(vehicle1);

        List<Driver> drivers = new ArrayList<>();
        Driver driver1 = new Driver();
        driver1.setId(20L);
        driver1.setLicenseNumber("LIC-001");
        drivers.add(driver1);

        ValhallaClient.MatrixResult matrix = createDummyMatrix(3);

        // When
        ORToolsAdapter.OptimizationResult result = orToolsAdapter.optimizeDeliveries(
                orders, vehicles, drivers, LocalDateTime.now(), matrix);

        // Then
        assertNotNull(result);
        assertFalse(result.getRoutes().isEmpty(), "Des routes d'optimisation auraient dû être générées.");
        
        ORToolsAdapter.RouteSolution solution = result.getRoutes().get(0);
        assertEquals(10L, solution.getVehicleId());
        assertEquals(20L, solution.getDriverId());
        assertEquals(2, solution.getOrderIds().size());
        assertTrue(solution.getOrderIds().contains(101L));
        assertTrue(solution.getOrderIds().contains(102L));
        
        assertTrue(solution.getLoadedWeightKg() <= vehicle1.getMaxPayloadKg(), "Le poids chargé dépasse la capacité.");
        assertTrue(solution.getLoadedVolumeM2() <= vehicle1.getMaxVolumeM2(), "Le volume chargé dépasse la capacité.");
        assertTrue(result.getTotalDistance() > 0.0, "La distance totale devrait être positive.");
    }

    @Test
    public void testOptimizeDeliveries_DroppedNodesDueToCapacity() {
        // Given
        List<Order> orders = new ArrayList<>();
        
        // Commande 1 dépasse le poids du véhicule
        Order order1 = new Order();
        order1.setId(101L);
        order1.setOrderNumber("ORD-001");
        order1.setWeightKg(600.0); // > 500kg max vehicle1
        order1.setVolumeM2(0.5);
        order1.setDeliveryLatitude(33.9800);
        order1.setDeliveryLongitude(-6.8400);
        order1.setPriority(OrderPriority.HIGH);
        orders.add(order1);

        // Commande 2 peut être livrée
        Order order2 = new Order();
        order2.setId(102L);
        order2.setOrderNumber("ORD-002");
        order2.setWeightKg(150.0);
        order2.setVolumeM2(0.8);
        order2.setDeliveryLatitude(33.9600);
        order2.setDeliveryLongitude(-6.8600);
        order2.setPriority(OrderPriority.NORMAL);
        orders.add(order2);

        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setId(10L);
        vehicle1.setMaxPayloadKg(500.0);
        vehicle1.setMaxVolumeM2(2.0);
        vehicles.add(vehicle1);

        List<Driver> drivers = new ArrayList<>();
        Driver driver1 = new Driver();
        driver1.setId(20L);
        drivers.add(driver1);

        ValhallaClient.MatrixResult matrix = createDummyMatrix(3);

        // When
        ORToolsAdapter.OptimizationResult result = orToolsAdapter.optimizeDeliveries(
                orders, vehicles, drivers, LocalDateTime.now(), matrix);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRoutes().size(), "Il devrait y avoir une seule route.");
        assertEquals(1, result.getUnassignedOrderIds().size(), "Il devrait y avoir une seule commande non assignée.");
        assertEquals(101L, result.getUnassignedOrderIds().get(0), "La commande 101 doit être rejetée (surcharge).");
        assertEquals(102L, result.getRoutes().get(0).getOrderIds().get(0), "La commande 102 doit être affectée.");
    }

    @Test
    public void testOptimizeDeliveries_EmptyOrders() {
        List<Order> orders = new ArrayList<>();
        List<Vehicle> vehicles = new ArrayList<>();
        List<Driver> drivers = new ArrayList<>();
        ValhallaClient.MatrixResult matrix = createDummyMatrix(1);

        assertThrows(OptimizationException.class, () -> {
            orToolsAdapter.optimizeDeliveries(orders, vehicles, drivers, LocalDateTime.now(), matrix);
        });
    }
}

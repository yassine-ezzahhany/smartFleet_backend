package ma.smartfleet.backend.service;

import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.infrastructure.adapter.ORToolsAdapter;
import ma.smartfleet.backend.model.*;
import ma.smartfleet.backend.model.enums.DeliveryProgramStatus;
import ma.smartfleet.backend.model.enums.OrderStatus;
import ma.smartfleet.backend.model.enums.SubProgramStatus;
import ma.smartfleet.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DeliveryOptimizationServiceTest {

    @Mock
    private ValhallaClient valhallaClient;
    @Mock
    private DeliveryProgramRepository deliveryProgramRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private SubProgramRepository subProgramRepository;
    @Mock
    private ORToolsAdapter orToolsAdapter;

    private DeliveryOptimizationService deliveryOptimizationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        deliveryOptimizationService = new DeliveryOptimizationService(
                valhallaClient,
                deliveryProgramRepository,
                orderRepository,
                driverRepository,
                vehicleRepository,
                subProgramRepository,
                orToolsAdapter
        );
        ReflectionTestUtils.setField(deliveryOptimizationService, "depotLat", 33.9716);
        ReflectionTestUtils.setField(deliveryOptimizationService, "depotLon", -6.8498);
    }

    @Test
    public void testOptimizeProgram_RemovesOldSubProgramsAndAvoidsHibernateException() {
        // Arrange
        Manager manager = new Manager();
        manager.setId(1L);

        DeliveryProgram program = new DeliveryProgram();
        program.setId(10L);
        program.setProgramNumber("PRG-001");
        program.setManager(manager);
        program.setStatus(DeliveryProgramStatus.PENDING);
        program.setPlannedDate(LocalDateTime.now());

        // Existing orders
        Order order1 = new Order();
        order1.setId(201L);
        order1.setOrderNumber("ORD-001");
        order1.setStatus(OrderStatus.ASSIGNED);
        order1.setDeliveryLatitude(33.9800);
        order1.setDeliveryLongitude(-6.8400);

        Order order2 = new Order();
        order2.setId(202L);
        order2.setOrderNumber("ORD-002");
        order2.setStatus(OrderStatus.ASSIGNED);
        order2.setDeliveryLatitude(33.9600);
        order2.setDeliveryLongitude(-6.8600);

        // Existing subprogram
        SubProgram oldSubProgram = new SubProgram();
        oldSubProgram.setId(100L);
        oldSubProgram.setSubProgramNumber("PRG-001-SUB-1");
        oldSubProgram.setDeliveryProgram(program);
        
        Set<Order> oldSubProgramOrders = new HashSet<>();
        oldSubProgramOrders.add(order1);
        oldSubProgramOrders.add(order2);
        oldSubProgram.setOrders(oldSubProgramOrders);
        
        order1.setSubProgram(oldSubProgram);
        order2.setSubProgram(oldSubProgram);

        Set<SubProgram> oldSubPrograms = new HashSet<>();
        oldSubPrograms.add(oldSubProgram);
        program.setSubPrograms(oldSubPrograms);

        Set<Order> programOrders = new HashSet<>();
        programOrders.add(order1);
        programOrders.add(order2);
        program.setOrders(programOrders);

        // Drivers and Vehicles
        Driver driver = new Driver();
        driver.setId(30L);
        List<Driver> drivers = Collections.singletonList(driver);

        Vehicle vehicle = new Vehicle();
        vehicle.setId(40L);
        List<Vehicle> vehicles = Collections.singletonList(vehicle);

        // Mock repository/client calls
        when(deliveryProgramRepository.findById(10L)).thenReturn(Optional.of(program));
        when(driverRepository.findByManagerIdAndAvailableTrue(1L)).thenReturn(drivers);
        when(vehicleRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(vehicles);

        ValhallaClient.MatrixResult matrixResult = new ValhallaClient.MatrixResult(
                new double[][]{{0, 1000, 1500}, {1000, 0, 800}, {1500, 800, 0}},
                new double[][]{{0, 120, 180}, {120, 0, 90}, {180, 90, 0}}
        );
        when(valhallaClient.getMatrix(any())).thenReturn(matrixResult);

        // Solver result with 1 new route solution (contains order1) and 1 unassigned (order2)
        List<ORToolsAdapter.RouteSolution> newRoutes = new ArrayList<>();
        ORToolsAdapter.RouteSolution route = new ORToolsAdapter.RouteSolution(
                40L, 30L, Collections.singletonList(201L), 1000.0, 120.0, 100.0, 0.5
        );
        newRoutes.add(route);
        ORToolsAdapter.OptimizationResult optimizationResult = new ORToolsAdapter.OptimizationResult(
                newRoutes, Collections.singletonList(202L), 1000.0, 120.0
        );
        when(orToolsAdapter.optimizeDeliveries(any(), any(), any(), any(), any()))
                .thenReturn(optimizationResult);

        // Saved SubProgram mock
        SubProgram newSubProgram = new SubProgram();
        newSubProgram.setId(101L);
        newSubProgram.setSubProgramNumber("PRG-001-SUB-1");
        newSubProgram.setDeliveryProgram(program);
        newSubProgram.setOrders(new HashSet<>());
        when(subProgramRepository.save(any(SubProgram.class))).thenReturn(newSubProgram);

        when(deliveryProgramRepository.save(any(DeliveryProgram.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DeliveryProgram result = deliveryOptimizationService.optimizeProgram(10L);

        // Assert
        assertNotNull(result);
        assertEquals(DeliveryProgramStatus.OPTIMIZED, result.getStatus());
        
        // Check that old subprogram reference was removed/cleared
        assertFalse(result.getSubPrograms().contains(oldSubProgram));
        assertEquals(1, result.getSubPrograms().size());
        assertTrue(result.getSubPrograms().contains(newSubProgram));

        // Verify order1 is now pointing to the new subprogram
        assertEquals(newSubProgram, order1.getSubProgram());
        assertEquals(OrderStatus.ASSIGNED, order1.getStatus());

        // Verify order2 was dissociated and set to UNASSIGNED
        assertNull(order2.getSubProgram());
        assertEquals(OrderStatus.UNASSIGNED, order2.getStatus());

        verify(orderRepository, atLeastOnce()).save(order1);
        verify(orderRepository, atLeastOnce()).save(order2);
    }
}

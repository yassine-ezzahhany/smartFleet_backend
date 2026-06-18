# Guide d'Implémentation SmartFleet Backend

## 📋 Vue d'ensemble

Ce guide fournit les directives pour implémenter les différents services et configurations du backend.

## 🏗️ Structure d'Implémentation

### 1. Structure d'Implémentation des Services

Les classes d'implémentations se trouvent directement sous `ma.smartfleet.backend.service`.

```
service/
├── UserService.java             # Gestion des utilisateurs (inscription, DTOs, etc.)
├── DeliveryOptimizationService.java
├── RouteCalculationService.java
├── LocationTrackingService.java
├── OrderManagementService.java
└── NotificationService.java
```

## 🔄 Flux d'Implémentation

### Phase 1 : Services Métier Basiques

#### 1.1 Inscription et Connexion locale (UserService)
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public User register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà.");
        }
        
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setActive(true);
        
        return userRepository.save(user);
    }
}
```

#### 1.2 OrderManagementServiceImpl
```java
@Service
@Transactional
public class OrderManagementServiceImpl implements OrderManagementService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private SubProgramRepository subProgramRepository;
    
    @Override
    public Order approveOrderDelivery(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        
        order.setClientApproved(true);
        order.setClientApprovalTime(LocalDateTime.now());
        order.setStatus(OrderStatus.DELIVERED);
        
        Order saved = orderRepository.save(order);
        
        // Vérifier si le sous-programme est complet
        checkAndUpdateSubProgramCompletion(order.getSubProgram().getId());
        
        return saved;
    }
    
    @Override
    public boolean checkAndUpdateSubProgramCompletion(Long subProgramId) {
        SubProgram subProgram = subProgramRepository.findById(subProgramId)
            .orElseThrow();
        
        long approvedCount = subProgram.getOrders().stream()
            .filter(Order::getClientApproved)
            .count();
        
        boolean allApproved = approvedCount == subProgram.getTotalOrdersCount();
        
        if (allApproved) {
            subProgram.setStatus(SubProgramStatus.COMPLETED);
            subProgram.setEndTime(LocalDateTime.now());
            subProgramRepository.save(subProgram);
            return true;
        }
        
        return false;
    }
}
```

### Phase 2 : Services d'Intégration Externe

#### 2.1 DeliveryOptimizationServiceImpl
```java
@Service
@Transactional
public class DeliveryOptimizationServiceImpl implements DeliveryOptimizationService {
    
    @Autowired
    private ORToolsAdapter orToolsAdapter;
    
    @Autowired
    private SubProgramRepository subProgramRepository;
    
    @Autowired
    private DriverRepository driverRepository;
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Override
    public DeliveryProgram optimizeDeliveryProgram(DeliveryProgram program) {
        // 1. Récupérer les ressources disponibles
        List<Driver> availableDrivers = driverRepository
            .findByManagerIdAndAvailableTrue(program.getManager().getId());
        List<Vehicle> availableVehicles = vehicleRepository
            .findByManagerIdAndActiveTrue(program.getManager().getId());
        
        // 2. Valider que l'optimisation est possible
        if (!canOptimizeOrders(new ArrayList<>(program.getOrders()))) {
            throw new OptimizationException(
                "Cannot optimize: insufficient capacity");
        }
        
        // 3. Appeler OR-Tools
        ORToolsAdapter.OptimizationResult result = 
            orToolsAdapter.optimizeDeliveries(
                new ArrayList<>(program.getOrders()),
                availableVehicles,
                availableDrivers
            );
        
        // 4. Créer les sous-programmes
        for (ORToolsAdapter.RouteSolution route : result.routes) {
            SubProgram subProgram = new SubProgram();
            subProgram.setDeliveryProgram(program);
            subProgram.setDriver(driverRepository.findById(route.driverId).get());
            subProgram.setVehicle(vehicleRepository.findById(route.vehicleId).get());
            subProgram.setStatus(SubProgramStatus.PENDING);
            subProgram.setEstimatedDistanceKm(route.routeDistance);
            subProgram.setEstimatedDurationMinutes(route.routeTime);
            
            // Assigner les commandes
            for (Long orderId : route.orderIds) {
                Order order = orderRepository.findById(orderId).get();
                order.setSubProgram(subProgram);
                subProgram.getOrders().add(order);
            }
            
            subProgramRepository.save(subProgram);
        }
        
        // 5. Mettre à jour le statut du programme
        program.setStatus(DeliveryProgramStatus.OPTIMIZED);
        
        return program;
    }
}
```

#### 2.2 RouteCalculationServiceImpl
```java
@Service
@Transactional
public class RouteCalculationServiceImpl implements RouteCalculationService {
    
    @Autowired
    private ValhallaAdapter valhallaAdapter;
    
    @Autowired
    private SubProgramRepository subProgramRepository;
    
    @Override
    public SubProgram calculateOptimalRoute(SubProgram subProgram) {
        // 1. Récupérer les commandes du sous-programme
        List<Order> orders = new ArrayList<>(subProgram.getOrders());
        
        // 2. Construire les coordonnées (dépôt + livraisons)
        List<ValhallaAdapter.Coordinate> coordinates = new ArrayList<>();
        
        // Ajouter le dépôt comme point de départ
        coordinates.add(new ValhallaAdapter.Coordinate(33.9716, -6.8498));
        
        // Ajouter les points de livraison
        for (Order order : orders) {
            coordinates.add(new ValhallaAdapter.Coordinate(
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude()
            ));
        }
        
        // 3. Appeler Valhalla pour calculer l'itinéraire
        ValhallaAdapter.RouteResponse response = 
            valhallaAdapter.optimizeRoute(33.9716, -6.8498, coordinates);
        
        // 4. Mettre à jour le sous-programme avec les résultats
        if (response.routes != null && !response.routes.isEmpty()) {
            ValhallaAdapter.Route route = response.routes.get(0);
            
            subProgram.setPolyline(route.geometry);
            subProgram.setEstimatedDistanceKm(route.distance / 1000.0);
            subProgram.setEstimatedDurationMinutes((int)(route.duration / 60));
        }
        
        return subProgramRepository.save(subProgram);
    }
}
```

#### 2.3 LocationTrackingServiceImpl
```java
@Service
public class LocationTrackingServiceImpl implements LocationTrackingService {
    
    @Autowired
    private DriverRepository driverRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    private final Map<Long, Long> lastBroadcastTime = new ConcurrentHashMap<>();
    private final long BROADCAST_INTERVAL_MS = 120000; // 2 minutes
    
    @Override
    public void updateDriverLocation(Long driverId, Double latitude, 
                                    Double longitude, Long timestamp) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow();
        
        // 1. Mettre à jour la position
        driver.setCurrentLatitude(latitude);
        driver.setCurrentLongitude(longitude);
        driver.setLastLocationUpdate(timestamp);
        
        // 2. Créer le Point PostGIS
        Point location = createPoint(latitude, longitude);
        driver.setCurrentLocation(location);
        
        driverRepository.save(driver);
        
        // 3. Diffuser aux clients autorisés (limitation de fréquence 2 min)
        long now = System.currentTimeMillis();
        long lastTime = lastBroadcastTime.getOrDefault(driverId, 0L);
        
        if (now - lastTime >= BROADCAST_INTERVAL_MS) {
            messagingTemplate.convertAndSend(
                "/topic/driver-locations",
                new DriverLocationUpdateDTO(driverId, latitude, longitude, timestamp)
            );
            lastBroadcastTime.put(driverId, now);
        }
    }
    
    @Override
    public boolean isDriverNearby(Long driverId, Double deliveryLatitude, 
                                 Double deliveryLongitude, Double radiusKm) {
        Driver driver = driverRepository.findById(driverId).orElseThrow();
        
        if (driver.getCurrentLocation() == null) {
            return false;
        }
        
        // Utiliser PostGIS pour calculer la distance
        Point deliveryPoint = createPoint(deliveryLatitude, deliveryLongitude);
        double distanceKm = calculateDistance(
            driver.getCurrentLatitude(), driver.getCurrentLongitude(),
            deliveryLatitude, deliveryLongitude
        );
        
        return distanceKm <= radiusKm;
    }
    
    private Point createPoint(Double latitude, Double longitude) {
        // Créer un Point GIS avec SRID 4326 (WGS84)
        CoordinateSequence coords = new CoordinateArraySequence(
            new Coordinate[] { new Coordinate(longitude, latitude) });
        return new Point(coords, new GeometryFactory(
            new PrecisionModel(), 4326));
    }
}
```

#### 2.4 NotificationServiceImpl
```java
@Service
public class NotificationServiceImpl implements NotificationService {
    
    @Autowired
    private FirebaseMessaging firebaseMessaging;
    
    @Value("${firebase.notification.enabled}")
    private Boolean notificationsEnabled;
    
    @Override
    public void notifyClientArrivingSoon(Client client, String driverName, 
                                        Integer estimatedArrivalMinutes) {
        if (!notificationsEnabled || client.getFirebaseDeviceToken() == null) {
            return;
        }
        
        try {
            Message message = Message.builder()
                .setToken(client.getFirebaseDeviceToken())
                .setNotification(Notification.builder()
                    .setTitle("Arrivée imminente")
                    .setBody(String.format(
                        "%s arrivera dans %d minutes",
                        driverName, estimatedArrivalMinutes
                    ))
                    .build())
                .putData("type", "arriving_soon")
                .putData("driverId", String.valueOf(/* driverId */))
                .build();
            
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            // Logger l'erreur
        }
    }
}
```

### Phase 3 : Mappers (DTO <-> Entité)

#### 3.1 OrderMapper
```java
@Mapper(componentModel = "spring")
public interface OrderMapper {
    
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "status", source = "status", 
             qualifiedByName = "statusToString")
    OrderDTO toDTO(Order order);
    
    @Named("statusToString")
    default String statusToString(OrderStatus status) {
        return status != null ? status.toString() : null;
    }
}
```

## 🧪 Tests d'Intégration

```java
@SpringBootTest
public class DeliveryOptimizationServiceTest {
    
    @Autowired
    private DeliveryOptimizationService service;
    
    @MockBean
    private ORToolsAdapter orToolsAdapter;
    
    @Test
    public void testOptimizeDeliveryProgram() {
        // Arrange
        DeliveryProgram program = // ... créer un programme de test
        
        // Mock OR-Tools
        when(orToolsAdapter.optimizeDeliveries(...))
            .thenReturn(/* résultat mocké */);
        
        // Act
        DeliveryProgram optimized = service.optimizeDeliveryProgram(program);
        
        // Assert
        assertThat(optimized.getStatus())
            .isEqualTo(DeliveryProgramStatus.OPTIMIZED);
        assertThat(optimized.getSubPrograms())
            .isNotEmpty();
    }
}
```

## 🔐 Configuration de Sécurité

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        
        return http.build();
    }
}
```

## 🗓️ Calendrier d'Implémentation Recommandé

- **Semaine 1:** Sécurité Spring Security + JWT local & UserService / AuthController
- **Semaine 2:** LocationTrackingService + NotificationService
- **Semaine 3:** DeliveryOptimizationService (OR-Tools)
- **Semaine 4:** RouteCalculationService (Valhalla) + Mappers
- **Semaine 5:** Contrôleurs REST + WebSocket
- **Semaine 6:** Tests + Documentation + Déploiement

## 🚨 Points Critiques

1. ✅ **Transactions:** Utiliser `@Transactional` pour la cohérence
2. ✅ **Validation:** Valider les données d'entrée
3. ✅ **Logs:** Logger les opérations critiques
4. ✅ **Exceptions:** Gérer proprement les erreurs externes (Valhalla, OR-Tools)
5. ✅ **Performance:** Indexer les requêtes fréquentes en BD
6. ✅ **Sécurité:** Vérifier les autorisations à chaque endpoint

---

**Version:** 1.0  
**Dernière mise à jour:** 2026-05-19

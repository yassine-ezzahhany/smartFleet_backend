package ma.smartfleet.backend.controller;

import lombok.RequiredArgsConstructor;
import ma.smartfleet.backend.dto.OrderDTO;
import ma.smartfleet.backend.model.Order;
import ma.smartfleet.backend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        Order order = orderService.createOrder(orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(order));
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<Order> orders = orderService.findAll();
        List<OrderDTO> dtos = orders.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(toDto(order));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<OrderDTO> approveDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(orderService.approveDelivery(id)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long id,
                                                      @RequestParam String status) {
        Order order = orderService.updateStatus(id, status);
        return ResponseEntity.ok(toDto(order));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OrderDTO> rejectOrder(@PathVariable Long id,
                                                @RequestBody String reason) {
        return ResponseEntity.ok(toDto(orderService.rejectOrder(id, reason)));
    }

    // @GetMapping("/test")
    // public List<OrderDTO> testOrders() {
    //     // --- ONGLET 1 & 3 : COMMANDES EN COURS / LIVRAISON AUJOURD'HUI (Dashboard & Map Tracking) ---
    //     OrderDTO order1 = new OrderDTO(
    //             1L,
    //             "ORD-2026-001",
    //             2L,
    //             120.5,
    //             12.0,
    //             33.5731, // Casablanca (Maarif)
    //             -7.5898,
    //             "Boulevard Al Massira Al Khadra, Maarif, Casablanca",
    //             "Livraison urgente - Entrepôt Central Sapino",
    //             "IN_PROGRESS", // En cours de route
    //             "35 min",
    //             null,
    //             false,
    //             "2026-06-12T08:30:00"
    //     );

    //     OrderDTO order2 = new OrderDTO(
    //             2L,
    //             "ORD-2026-002",
    //             2L,
    //             45.0,
    //             4.5,
    //             33.0012, // Settat
    //             -7.6166,
    //             "Zone Industrielle, Settat",
    //             "Déposer au quai de déchargement N°3",
    //             "PENDING", // Commande créée, en attente de traitement
    //             "2 heures",
    //             null,
    //             false,
    //             "2026-06-12T09:15:00"
    //     );

    //     // --- ONGLET 2 : HISTORIQUE DES COMMANDES (Livrées & Terminées) ---
    //     OrderDTO order3 = new OrderDTO(
    //             3L,
    //             "ORD-2026-991",
    //             2L,
    //             500.0,
    //             35.0,
    //             34.0208, // Rabat
    //             -6.8361,
    //             "Avenue Mohammed V, Agdal, Rabat",
    //             "Livraison standard de marchandises",
    //             "DELIVERED", // Statut d'historique
    //             null,
    //             "2026-06-10T14:45:00",
    //             true,
    //             "2026-06-10T09:00:00"
    //     );

    //     OrderDTO order4 = new OrderDTO(
    //             4L,
    //             "ORD-2026-992",
    //             2L,
    //             85.0,
    //             6.0,
    //             33.5892, // Casablanca (Centre)
    //             -7.6041,
    //             "Boulevard Mohammed V, Centre-Ville, Casablanca",
    //             "Colis fragile - À manipuler avec soin",
    //             "DELIVERED",
    //             null,
    //             "2026-06-08T11:20:00",
    //             true,
    //             "2026-06-08T08:00:00"
                
    //     );

    //     OrderDTO order5 = new OrderDTO(
    //             5L,
    //             "ORD-2026-993",
    //             2L,
    //             210.0,
    //             18.5,
    //             34.0331, // Salé
    //             -6.7984,
    //             "Quartier Karima, Salé",
    //             "Matériaux de construction légers",
    //             "DELIVERED",
    //             null,
    //             "2026-06-05T17:10:00",
    //             true,
    //             "2026-06-05T10:30:00"
    //     );

    //     return List.of(order1, order2, order3, order4, order5);
    // 
@GetMapping("/test")
public List<OrderDTO> testOrders() {
    // 1. Récupère dynamiquement toutes les commandes de la BDD via le service
    List<Order> orders = orderService.findAll();

    // 2. Convertit la liste d'entités Order en liste de OrderDTO
    return orders.stream()
                 .map(this::toDto)
                 .toList();
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
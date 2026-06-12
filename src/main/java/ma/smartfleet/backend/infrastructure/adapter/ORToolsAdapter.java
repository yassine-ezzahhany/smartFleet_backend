package ma.smartfleet.backend.infrastructure.adapter;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.exception.OptimizationException;
import ma.smartfleet.backend.model.Driver;
import ma.smartfleet.backend.model.Order;
import ma.smartfleet.backend.model.Vehicle;
import ma.smartfleet.backend.model.enums.OrderPriority;
import ma.smartfleet.backend.service.ValhallaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adaptateur d'intégration avec Google OR-Tools pour résoudre le Vehicle Routing Problem (VRP).
 */
@Component
@Slf4j
public class ORToolsAdapter {

    static {
        try {
            log.info("Chargement des bibliothèques natives Google OR-Tools...");
            Loader.loadNativeLibraries();
            log.info("Bibliothèques natives Google OR-Tools chargées avec succès !");
        } catch (Throwable e) {
            log.error("Échec du chargement des bibliothèques natives Google OR-Tools : ", e);
        }
    }

    @Value("${app.depot.latitude:33.9716}")
    private double depotLat;

    @Value("${app.depot.longitude:-6.8498}")
    private double depotLon;

    @Value("${ortools.optimization.time-limit-seconds:30}")
    private int timeLimitSeconds;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RouteSolution {
        private Long vehicleId;
        private Long driverId;
        private List<Long> orderIds = new ArrayList<>();
        private Double routeDistance; // en mètres
        private Double routeTime;     // en secondes
        private Double loadedWeightKg;
        private Double loadedVolumeM2;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OptimizationResult {
        private List<RouteSolution> routes = new ArrayList<>();
        private List<Long> unassignedOrderIds = new ArrayList<>();
        private Double totalDistance = 0.0;
        private Double totalTime = 0.0;
    }

    /**
     * Résout le VRP pour attribuer de façon optimale les commandes aux véhicules/conducteurs.
     */
    public OptimizationResult optimizeDeliveries(
            List<Order> orders,
            List<Vehicle> vehicles,
            List<Driver> drivers,
            LocalDateTime plannedDate,
            ValhallaClient.MatrixResult matrixResult) {

        if (orders == null || orders.isEmpty()) {
            throw new OptimizationException("Aucune commande à optimiser.");
        }
        if (vehicles == null || vehicles.isEmpty()) {
            throw new OptimizationException("Aucun véhicule actif disponible.");
        }
        if (drivers == null || drivers.isEmpty()) {
            throw new OptimizationException("Aucun chauffeur disponible.");
        }

        int numVehicles = Math.min(vehicles.size(), drivers.size());
        int numOrders = orders.size();
        int numNodes = numOrders + 1; // Le dépôt (index 0) + les commandes (index 1 à numOrders)

        log.info("Lancement de l'optimisation OR-Tools : {} commandes, {} véhicules, {} chauffeurs.",
                numOrders, vehicles.size(), drivers.size());

        // Récupérer les matrices de distance et durée de Valhalla
        double[][] distanceMatrix = matrixResult.distances();
        double[][] durationMatrix = matrixResult.durations();

        try {
            // 2. Création du gestionnaire de routage
            RoutingIndexManager manager = new RoutingIndexManager(numNodes, numVehicles, 0);
            RoutingModel routing = new RoutingModel(manager);

            // 3. Enregistrement de la contrainte de poids (Capacité en kg, représentée en grammes)
            final int weightCallbackIndex = routing.registerUnaryTransitCallback(
                (long fromIndex) -> {
                    int node = manager.indexToNode(fromIndex);
                    if (node == 0) return 0L;
                    return (long) Math.round(orders.get(node - 1).getWeightKg() * 1000.0);
                }
            );
            long[] weightCapacities = new long[numVehicles];
            for (int i = 0; i < numVehicles; i++) {
                weightCapacities[i] = (long) Math.round(vehicles.get(i).getMaxPayloadKg() * 1000.0);
            }
            routing.addDimensionWithVehicleCapacity(
                weightCallbackIndex,
                0L, // sans marge
                weightCapacities,
                true, // cumul démarre à 0
                "Weight"
            );

            // 4. Enregistrement de la contrainte de volume (Capacité en m2, multipliée par 1000)
            final int volumeCallbackIndex = routing.registerUnaryTransitCallback(
                (long fromIndex) -> {
                    int node = manager.indexToNode(fromIndex);
                    if (node == 0) return 0L;
                    return (long) Math.round(orders.get(node - 1).getVolumeM2() * 1000.0);
                }
            );
            long[] volumeCapacities = new long[numVehicles];
            for (int i = 0; i < numVehicles; i++) {
                volumeCapacities[i] = (long) Math.round(vehicles.get(i).getMaxVolumeM2() * 1000.0);
            }
            routing.addDimensionWithVehicleCapacity(
                volumeCallbackIndex,
                0L, // sans marge
                volumeCapacities,
                true, // cumul démarre à 0
                "Volume"
            );

            // 5. Enregistrement du coût principal (Temps de parcours = durée de transport + déchargement)
            final int timeCallbackIndex = routing.registerTransitCallback(
                (long fromIndex, long toIndex) -> {
                    int fromNode = manager.indexToNode(fromIndex);
                    int toNode = manager.indexToNode(toIndex);
                    long travelTime = (long) Math.round(durationMatrix[fromNode][toNode]);
                    long serviceTime = (fromNode == 0) ? 0L : 600L; // 10 minutes de déchargement
                    return travelTime + serviceTime;
                }
            );
            routing.setArcCostEvaluatorOfAllVehicles(timeCallbackIndex); // Temps de parcours = coût principal!

            // 6. Gestion des commandes impossibles (Disjunctions) et priorités
            for (int i = 1; i < numNodes; i++) {
                long index = manager.nodeToIndex(i);
                Order order = orders.get(i - 1);
                
                long penalty = 1000000L; // 1M par défaut
                if (order.getPriority() == OrderPriority.HIGH) {
                    penalty = 10000000L; // 10M
                } else if (order.getPriority() == OrderPriority.LOW) {
                    penalty = 100000L; // 100k
                }
                routing.addDisjunction(new long[] { index }, penalty);
            }

            // 7. Paramètres de recherche
            RoutingSearchParameters searchParameters =
                main.defaultRoutingSearchParameters()
                    .toBuilder()
                    .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                    .setTimeLimit(com.google.protobuf.Duration.newBuilder().setSeconds(timeLimitSeconds).build())
                    .build();

            // 8. Résolution
            Assignment solution = routing.solveWithParameters(searchParameters);

            if (solution == null) {
                log.warn("Aucune solution d'optimisation VRP trouvée par OR-Tools. Repli requis.");
                throw new OptimizationException("Le solveur n'a pas pu trouver de solution d'optimisation valide avec les contraintes spécifiées.");
            }

            // 9. Extraction du résultat
            OptimizationResult result = new OptimizationResult();
            double totalDistance = 0.0;
            double totalTime = 0.0;
            Set<Long> assignedOrderIds = new HashSet<>();

            for (int i = 0; i < numVehicles; i++) {
                RouteSolution route = new RouteSolution();
                route.setVehicleId(vehicles.get(i).getId());
                route.setDriverId(drivers.get(i).getId());

                long index = routing.start(i);
                double routeDistance = 0.0;
                double routeTime = 0.0;
                double loadedWeight = 0.0;
                double loadedVolume = 0.0;

                while (!routing.isEnd(index)) {
                    int node = manager.indexToNode(index);
                    if (node != 0) {
                        Order o = orders.get(node - 1);
                        route.getOrderIds().add(o.getId());
                        assignedOrderIds.add(o.getId());
                        loadedWeight += o.getWeightKg();
                        loadedVolume += o.getVolumeM2();
                    }
                    long nextIndex = solution.value(routing.nextVar(index));
                    routeDistance += distanceMatrix[manager.indexToNode(index)][manager.indexToNode(nextIndex)];
                    routeTime += durationMatrix[manager.indexToNode(index)][manager.indexToNode(nextIndex)];
                    index = nextIndex;
                }

                if (!route.getOrderIds().isEmpty()) {
                    route.setRouteDistance(routeDistance);
                    route.setRouteTime(routeTime);
                    route.setLoadedWeightKg(loadedWeight);
                    route.setLoadedVolumeM2(loadedVolume);

                    result.getRoutes().add(route);
                    totalDistance += routeDistance;
                    totalTime += routeTime;
                }
            }

            // Commandes non assignées
            for (Order o : orders) {
                if (!assignedOrderIds.contains(o.getId())) {
                    result.getUnassignedOrderIds().add(o.getId());
                }
            }

            result.setTotalDistance(totalDistance);
            result.setTotalTime(totalTime);

            log.info("Optimisation réussie avec {} tournées créées et {} commandes non assignées. Distance totale = {}m",
                    result.getRoutes().size(), result.getUnassignedOrderIds().size(), totalDistance);
            return result;

        } catch (Exception ex) {
            log.error("Erreur lors de la résolution du VRP avec OR-Tools :", ex);
            throw new OptimizationException("Erreur interne du solveur d'optimisation : " + ex.getMessage(), ex);
        }
    }
}

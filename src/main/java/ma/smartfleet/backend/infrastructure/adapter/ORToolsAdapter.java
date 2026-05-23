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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        private Double routeTime;     // en secondes (estimé sur 50 km/h)
        private Double loadedWeightKg;
        private Double loadedVolumeM2;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OptimizationResult {
        private List<RouteSolution> routes = new ArrayList<>();
        private Double totalDistance = 0.0;
        private Double totalTime = 0.0;
    }

    /**
     * Résout le VRP pour attribuer de façon optimale les commandes aux véhicules/conducteurs.
     */
    public OptimizationResult optimizeDeliveries(
            List<Order> orders,
            List<Vehicle> vehicles,
            List<Driver> drivers) {

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

        // 1. Calculer la matrice de distances (repli de sécurité en Haversine)
        long[][] distanceMatrix = new long[numNodes][numNodes];
        for (int i = 0; i < numNodes; i++) {
            double fromLat = (i == 0) ? depotLat : orders.get(i - 1).getDeliveryLatitude();
            double fromLon = (i == 0) ? depotLon : orders.get(i - 1).getDeliveryLongitude();
            for (int j = 0; j < numNodes; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                } else {
                    double toLat = (j == 0) ? depotLat : orders.get(j - 1).getDeliveryLatitude();
                    double toLon = (j == 0) ? depotLon : orders.get(j - 1).getDeliveryLongitude();
                    // Haversine en mètres multiplié par 1.3 pour simuler le réseau routier
                    distanceMatrix[i][j] = (long) Math.round(calculateHaversineDistance(fromLat, fromLon, toLat, toLon) * 1.3);
                }
            }
        }

        try {
            // 2. Création du gestionnaire de routage
            RoutingIndexManager manager = new RoutingIndexManager(numNodes, numVehicles, 0);
            RoutingModel routing = new RoutingModel(manager);

            // 3. Enregistrement du callback de transit (distance)
            final int transitCallbackIndex = routing.registerTransitCallback(
                (long fromIndex, long toIndex) -> {
                    int fromNode = manager.indexToNode(fromIndex);
                    int toNode = manager.indexToNode(toIndex);
                    return distanceMatrix[fromNode][toNode];
                }
            );
            routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

            // 4. Enregistrement de la contrainte de poids (Capacité en kg, représentée en grammes)
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

            // 5. Enregistrement de la contrainte de volume (Capacité en m2, multipliée par 1000 pour conserver la précision)
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

            // 6. Paramètres de recherche
            RoutingSearchParameters searchParameters =
                main.defaultRoutingSearchParameters()
                    .toBuilder()
                    .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                    .setTimeLimit(com.google.protobuf.Duration.newBuilder().setSeconds(timeLimitSeconds).build())
                    .build();

            // 7. Résolution
            Assignment solution = routing.solveWithParameters(searchParameters);

            if (solution == null) {
                log.warn("Aucune solution d'optimisation VRP trouvée par OR-Tools. Repli requis.");
                throw new OptimizationException("Le solveur n'a pas pu trouver de solution d'optimisation valide avec les contraintes spécifiées.");
            }

            // 8. Extraction du résultat
            OptimizationResult result = new OptimizationResult();
            double totalDistance = 0.0;
            double totalTime = 0.0;

            for (int i = 0; i < numVehicles; i++) {
                RouteSolution route = new RouteSolution();
                route.setVehicleId(vehicles.get(i).getId());
                route.setDriverId(drivers.get(i).getId());

                long index = routing.start(i);
                double routeDistance = 0.0;
                double loadedWeight = 0.0;
                double loadedVolume = 0.0;

                while (!routing.isEnd(index)) {
                    int node = manager.indexToNode(index);
                    if (node != 0) {
                        Order o = orders.get(node - 1);
                        route.getOrderIds().add(o.getId());
                        loadedWeight += o.getWeightKg();
                        loadedVolume += o.getVolumeM2();
                    }
                    long nextIndex = solution.value(routing.nextVar(index));
                    routeDistance += routing.getArcCostForVehicle(index, nextIndex, i);
                    index = nextIndex;
                }

                // Ne garder la route que si elle contient au moins une commande
                if (!route.getOrderIds().isEmpty()) {
                    route.setRouteDistance(routeDistance);
                    // Estimation temps : distance / (vitesse moyenne de 50km/h = 13.88 m/s)
                    double routeTime = routeDistance / 13.88;
                    route.setRouteTime(routeTime);
                    route.setLoadedWeightKg(loadedWeight);
                    route.setLoadedVolumeM2(loadedVolume);

                    result.getRoutes().add(route);
                    totalDistance += routeDistance;
                    totalTime += routeTime;
                }
            }

            result.setTotalDistance(totalDistance);
            result.setTotalTime(totalTime);

            log.info("Optimisation réussie avec {} tournées créées. Distance totale = {}m",
                    result.getRoutes().size(), totalDistance);
            return result;

        } catch (Exception ex) {
            log.error("Erreur lors de la résolution du VRP avec OR-Tools :", ex);
            throw new OptimizationException("Erreur interne du solveur d'optimisation : " + ex.getMessage(), ex);
        }
    }

    /**
     * Calcule la distance de Haversine en mètres entre deux points géographiques.
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000.0; // en mètres
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}

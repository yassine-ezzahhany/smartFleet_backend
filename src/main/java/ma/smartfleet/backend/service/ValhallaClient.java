package ma.smartfleet.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.smartfleet.backend.exception.ValhallaServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Client REST pour l'intégration avec Valhalla.
 * Encapsulé directement dans la couche service puisqu'on utilise une architecture en couches.
 */
@Component
@Slf4j
public class ValhallaClient {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Duration timeout;

    public record Coordinate(Double lat, Double lon) {}
    public record Route(String geometry, Double distance, Double duration) {}
    public record Waypoint(Double lat, Double lon, Integer index) {}
    public record RouteRequest(List<Coordinate> coordinates, String costing) {
        public RouteRequest(List<Coordinate> coordinates) { this(coordinates, "auto"); }
    }
    public record RouteResponse(List<Route> routes, List<Waypoint> waypoints) {}

    public ValhallaClient(WebClient.Builder webClientBuilder,
                          @Value("${valhalla.service.url}") String baseUrl,
                          @Value("${valhalla.service.timeout-seconds:30}") Integer timeoutSeconds) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public RouteResponse calculateRoute(RouteRequest request) {
        try {
            JsonNode body = buildLocationsPayload(request.coordinates(), request.costing());
            JsonNode json = post("/route", body);
            return parseRouteResponse(json);
        } catch (Exception ex) {
            throw new ValhallaServiceException("Failed to calculate route", ex);
        }
    }

    public RouteResponse optimizeRoute(Double startLat, Double startLon, List<Coordinate> coordinates) {
        try {
            List<Coordinate> all = new ArrayList<>();
            all.add(new Coordinate(startLat, startLon));
            all.addAll(coordinates);

            JsonNode body = buildLocationsPayload(all, "auto");
            JsonNode json = post("/optimized_route", body);

            if (json == null || json.isEmpty()) {
                return calculateRoute(new RouteRequest(all));
            }
            return parseRouteResponse(json);
        } catch (ValhallaServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            try { return calculateRoute(new RouteRequest(coordinates)); }
            catch (Exception e2) { throw new ValhallaServiceException("Failed to optimize route", ex); }
        }
    }

    public record MatrixResult(double[][] distances, double[][] durations) {}

    public MatrixResult getMatrix(List<Coordinate> coordinates) {
        int n = coordinates.size();
        double[][] distances = new double[n][n];
        double[][] durations = new double[n][n];

        // Remplir avec le fallback Haversine initialement
        for (int i = 0; i < n; i++) {
            Coordinate from = coordinates.get(i);
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    distances[i][j] = 0.0;
                    durations[i][j] = 0.0;
                } else {
                    Coordinate to = coordinates.get(j);
                    double dist = calculateHaversineDistance(from.lat(), from.lon(), to.lat(), to.lon()) * 1.3;
                    distances[i][j] = dist;
                    durations[i][j] = dist / 13.88; // 50 km/h en m/s (13.88 m/s)
                }
            }
        }

        try {
            log.info("Appel de Valhalla /sources_to_targets pour {} coordonnées...", n);
            JsonNode body = buildMatrixPayload(coordinates, coordinates, "auto");

            JsonNode json = post("/sources_to_targets", body);
            if (json != null) {
                JsonNode matrixNode = json.has("sources_to_targets") ? json.get("sources_to_targets") : json.get("matrix");
                if (matrixNode != null) {
                    for (int i = 0; i < n; i++) {
                        JsonNode row = matrixNode.get(i);
                        for (int j = 0; j < n; j++) {
                            JsonNode cell = row.get(j);
                            if (cell != null) {
                                if (cell.has("distance")) {
                                    distances[i][j] = cell.get("distance").asDouble();
                                }
                                if (cell.has("time")) {
                                    durations[i][j] = cell.get("time").asDouble();
                                } else if (cell.has("duration")) {
                                    durations[i][j] = cell.get("duration").asDouble();
                                }
                            }
                        }
                    }
                    log.info("Matrice Valhalla récupérée avec succès.");
                } else {
                    log.warn("Noeud matrix non trouvé dans la réponse, utilisation du fallback Haversine.");
                }
            } else {
                log.warn("Réponse Valhalla /sources_to_targets vide, utilisation du fallback Haversine.");
            }
        } catch (Exception ex) {
            log.error("Échec de l'appel Valhalla /sources_to_targets, utilisation du fallback Haversine : {}", ex.getMessage());
        }

        return new MatrixResult(distances, durations);
    }

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

    public double[][] calculateDistanceMatrix(List<Integer> sources, List<Integer> targets, List<Coordinate> coordinates) {
        try {
            List<Coordinate> sourceCoords = new ArrayList<>();
            for (int idx : sources) {
                sourceCoords.add(coordinates.get(idx));
            }
            List<Coordinate> targetCoords = new ArrayList<>();
            for (int idx : targets) {
                targetCoords.add(coordinates.get(idx));
            }

            JsonNode body = buildMatrixPayload(sourceCoords, targetCoords, "auto");

            JsonNode json = post("/sources_to_targets", body);
            if (json == null) throw new ValhallaServiceException("Empty matrix response");

            JsonNode distancesNode = json.has("sources_to_targets") ? json.get("sources_to_targets") : json.get("matrix");
            if (distancesNode == null) throw new ValhallaServiceException("No distance matrix in response");

            int s = sources.size(), t = targets.size();
            double[][] matrix = new double[s][t];
            for (int i = 0; i < s; i++) {
                JsonNode row = distancesNode.get(i);
                for (int j = 0; j < t; j++) {
                    JsonNode cell = row.get(j);
                    matrix[i][j] = cell.has("distance") ? cell.get("distance").asDouble() : cell.asDouble();
                }
            }
            return matrix;
        } catch (Exception ex) {
            throw new ValhallaServiceException("Failed to calculate distance matrix", ex);
        }
    }

    private JsonNode post(String uri, JsonNode body) {
        try {
            Mono<JsonNode> resp = webClient.post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(timeout);
            return resp.block(timeout.plusSeconds(1));
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException ex) {
            log.error("Valhalla API returned error status {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        }
    }

    private JsonNode buildLocationsPayload(List<Coordinate> coords, String costing) {
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode locs = mapper.createArrayNode();
        for (Coordinate c : coords) {
            com.fasterxml.jackson.databind.node.ObjectNode loc = mapper.createObjectNode();
            loc.put("lat", c.lat());
            loc.put("lon", c.lon());
            locs.add(loc);
        }
        root.set("locations", locs);
        root.put("costing", costing == null ? "auto" : costing);
        return root;
    }

    private JsonNode buildMatrixPayload(List<Coordinate> sources, List<Coordinate> targets, String costing) {
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        
        com.fasterxml.jackson.databind.node.ArrayNode srcNode = mapper.createArrayNode();
        for (Coordinate c : sources) {
            com.fasterxml.jackson.databind.node.ObjectNode loc = mapper.createObjectNode();
            loc.put("lat", c.lat());
            loc.put("lon", c.lon());
            srcNode.add(loc);
        }
        
        com.fasterxml.jackson.databind.node.ArrayNode tgtNode = mapper.createArrayNode();
        for (Coordinate c : targets) {
            com.fasterxml.jackson.databind.node.ObjectNode loc = mapper.createObjectNode();
            loc.put("lat", c.lat());
            loc.put("lon", c.lon());
            tgtNode.add(loc);
        }
        
        root.set("sources", srcNode);
        root.set("targets", tgtNode);
        root.put("costing", costing == null ? "auto" : costing);
        return root;
    }

    private RouteResponse parseRouteResponse(JsonNode json) {
        List<Route> routes = new ArrayList<>();
        List<Waypoint> waypoints = new ArrayList<>();

        if (json.has("trip")) {
            JsonNode trip = json.get("trip");
            JsonNode shape = trip.get("shape");
            double distance = trip.has("summary") && trip.get("summary").has("distance") ? trip.get("summary").get("distance").asDouble() : 0.0;
            double time = trip.has("summary") && trip.get("summary").has("time") ? trip.get("summary").get("time").asDouble() : 0.0;
            routes.add(new Route(shape != null ? shape.asText() : null, distance, time));
        } else if (json.has("routes")) {
            for (JsonNode r : json.get("routes")) {
                routes.add(new Route(
                        r.has("geometry") ? r.get("geometry").asText() : null,
                        r.has("distance") ? r.get("distance").asDouble() : 0.0,
                        r.has("duration") ? r.get("duration").asDouble() : 0.0
                ));
            }
        }

        if (json.has("waypoints")) {
            int idx = 0;
            for (JsonNode wp : json.get("waypoints")) {
                waypoints.add(new Waypoint(
                        wp.has("lat") ? wp.get("lat").asDouble() : 0.0,
                        wp.has("lon") ? wp.get("lon").asDouble() : 0.0,
                        idx++
                ));
            }
        }
        return new RouteResponse(routes, waypoints);
    }
}

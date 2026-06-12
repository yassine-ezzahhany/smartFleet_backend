package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.exception.ResourceNotFoundException;
import ma.smartfleet.backend.model.Order;
import ma.smartfleet.backend.model.SubProgram;
import ma.smartfleet.backend.model.enums.OrderStatus;
import ma.smartfleet.backend.repository.OrderRepository;
import ma.smartfleet.backend.repository.SubProgramRepository;
import ma.smartfleet.backend.service.ValhallaClient.Coordinate;
import ma.smartfleet.backend.service.ValhallaClient.RouteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final ValhallaClient valhallaClient;
    private final SubProgramRepository subProgramRepository;

    @Value("${app.depot.latitude}")
    private Double depotLat;

    @Value("${app.depot.longitude}")
    private Double depotLon;

    /**
     * Calcule l'itinéraire optimal pour un SubProgram et met à jour ses champs.
     */
    @Transactional
    public SubProgram calculateOptimalRoute(Long subProgramId) {
        SubProgram subProgram = subProgramRepository.findById(subProgramId)
                .orElseThrow(() -> new ResourceNotFoundException("SubProgram", subProgramId));

        List<Order> sortedOrders = subProgram.getOrders().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .sorted(Comparator.comparing(Order::getVisitSequence, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        List<Coordinate> routeCoords = new ArrayList<>();
        routeCoords.add(new Coordinate(depotLat, depotLon));
        for (Order o : sortedOrders) {
            routeCoords.add(new Coordinate(o.getDeliveryLatitude(), o.getDeliveryLongitude()));
        }
        routeCoords.add(new Coordinate(depotLat, depotLon));

        RouteResponse resp = valhallaClient.calculateRoute(new ValhallaClient.RouteRequest(routeCoords));
        if (resp != null && !resp.routes().isEmpty()) {
            ValhallaClient.Route r = resp.routes().get(0);
            subProgram.setPolyline(r.geometry());
            subProgram.setEstimatedDistanceKm(r.distance() != null ? r.distance() / 1000.0 : null);
            subProgram.setEstimatedDurationMinutes(r.duration() != null ? (int) Math.round(r.duration() / 60.0) : null);
        }

        subProgram.setTotalOrdersCount((int) subProgram.getOrders().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED).count());

        return subProgramRepository.save(subProgram);
    }

    /**
     * Calcule distance + durée entre une liste de coordonnées.
     */
    public record RouteMetrics(Double distanceKm, Integer durationMinutes, String polyline) {}

    public RouteMetrics calculateMetrics(List<double[]> latLonPairs) {
        List<Coordinate> coords = new ArrayList<>();
        for (double[] p : latLonPairs) coords.add(new Coordinate(p[0], p[1]));

        RouteResponse resp = valhallaClient.calculateRoute(new ValhallaClient.RouteRequest(coords));
        if (resp != null && !resp.routes().isEmpty()) {
            ValhallaClient.Route r = resp.routes().get(0);
            return new RouteMetrics(
                    r.distance() != null ? r.distance() / 1000.0 : 0.0,
                    r.duration() != null ? (int) Math.round(r.duration() / 60.0) : 0,
                    r.geometry()
            );
        }
        return new RouteMetrics(0.0, 0, null);
    }
}

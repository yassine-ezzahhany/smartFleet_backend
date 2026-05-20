package ma.smartfleet.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.exception.ResourceNotFoundException;
import ma.smartfleet.backend.model.Driver;
import ma.smartfleet.backend.repository.DriverRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationTrackingService {

    private final DriverRepository driverRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public void updateDriverLocation(Long driverId, Double latitude, Double longitude, Long timestamp) {
        log.debug("Location update driver={}: lat={}, lon={}", driverId, latitude, longitude);
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));

        driver.setCurrentLatitude(latitude);
        driver.setCurrentLongitude(longitude);
        driver.setLastLocationUpdate(timestamp);
        // PostGIS: (longitude, latitude)
        driver.setCurrentLocation(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        driverRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public Point getDriverLastLocation(Long driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId))
                .getCurrentLocation();
    }

    /**
     * Haversine formula — distance en kilomètres entre deux points GPS.
     */
    public double calculateDistanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Transactional(readOnly = true)
    public boolean isDriverNearby(Long driverId, Double deliveryLat, Double deliveryLon, Double radiusKm) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        if (driver.getCurrentLatitude() == null || driver.getCurrentLongitude() == null) return false;
        return calculateDistanceKm(driver.getCurrentLatitude(), driver.getCurrentLongitude(), deliveryLat, deliveryLon) <= radiusKm;
    }
}

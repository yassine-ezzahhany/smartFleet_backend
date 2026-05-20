package ma.smartfleet.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SmartFleet Backend — Layered Architecture
 *
 * Layers:
 *   model      → JPA entities + enums
 *   repository → Spring Data JPA repositories
 *   service    → business logic + external clients (Valhalla)
 *   controller → REST controllers
 *   config     → Spring configuration (Security, WebClient, WebSocket)
 *   exception  → custom exceptions + global handler
 *   dto        → request / response data transfer objects
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}

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

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner databaseMigrator(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Drop the not-null constraint on clerk_id column in case it still exists
                jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN clerk_id DROP NOT NULL");
                System.out.println("--- SCHEMA MIGRATION SUCCESS: clerk_id has been made nullable ---");
            } catch (Exception e) {
                System.out.println("--- SCHEMA MIGRATION: clerk_id already nullable or dropped: " + e.getMessage() + " ---");
            }
            try {
                // Drop the not-null constraint on first_name column in case it still exists
                jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN first_name DROP NOT NULL");
                System.out.println("--- SCHEMA MIGRATION SUCCESS: first_name has been made nullable ---");
            } catch (Exception e) {
                System.out.println("--- SCHEMA MIGRATION: first_name already nullable or dropped: " + e.getMessage() + " ---");
            }
            try {
                // Drop the not-null constraint on last_name column in case it still exists
                jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN last_name DROP NOT NULL");
                System.out.println("--- SCHEMA MIGRATION SUCCESS: last_name has been made nullable ---");
            } catch (Exception e) {
                System.out.println("--- SCHEMA MIGRATION: last_name already nullable or dropped: " + e.getMessage() + " ---");
            }
        };
    }
}

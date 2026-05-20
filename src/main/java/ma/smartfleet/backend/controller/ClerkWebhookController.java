package ma.smartfleet.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/webhooks/clerk")
@RequiredArgsConstructor
@Slf4j
public class ClerkWebhookController {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Value("${app.security.clerk.webhook-secret:}")
    private String webhookSecret;

    /**
     * Webhook appelé par Clerk pour les événements utilisateur.
     * Events: user.created, user.updated, user.deleted
     */
    @PostMapping("/events")
    public ResponseEntity<Void> handleClerkWebhook(@RequestBody String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            String type = event.get("type").asText();
            
            JsonNode data = event.get("data");
            if (data == null) {
                log.warn("Webhook event has no data");
                return ResponseEntity.badRequest().build();
            }

            switch (type) {
                case "user.created":
                    handleUserCreated(data);
                    break;
                case "user.updated":
                    handleUserUpdated(data);
                    break;
                case "user.deleted":
                    handleUserDeleted(data);
                    break;
                default:
                    log.debug("Ignoring webhook event type: {}", type);
            }

            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Error processing Clerk webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Traite l'événement user.created de Clerk.
     */
    private void handleUserCreated(JsonNode data) {
        String clerkId = data.get("id").asText();
        JsonNode emailAddresses = data.get("email_addresses");
        String email = emailAddresses.isArray() && emailAddresses.size() > 0
            ? emailAddresses.get(0).get("email_address").asText()
            : "";
        
        String firstName = data.has("first_name") ? data.get("first_name").asText("") : "";
        String lastName = data.has("last_name") ? data.get("last_name").asText("") : "";

        log.info("Creating user from Clerk webhook: {} ({})", clerkId, email);
        userService.getOrCreateUserFromClerk(clerkId, email, firstName, lastName);
    }

    /**
     * Traite l'événement user.updated de Clerk.
     */
    private void handleUserUpdated(JsonNode data) {
        String clerkId = data.get("id").asText();
        JsonNode emailAddresses = data.get("email_addresses");
        String email = emailAddresses.isArray() && emailAddresses.size() > 0
            ? emailAddresses.get(0).get("email_address").asText()
            : "";
        
        String firstName = data.has("first_name") ? data.get("first_name").asText("") : "";
        String lastName = data.has("last_name") ? data.get("last_name").asText("") : "";

        log.info("Updating user from Clerk webhook: {} ({})", clerkId, email);
        userService.syncUserFromClerk(clerkId, email, firstName, lastName, true);
    }

    /**
     * Traite l'événement user.deleted de Clerk.
     */
    private void handleUserDeleted(JsonNode data) {
        String clerkId = data.get("id").asText();
        log.info("Deactivating user from Clerk webhook: {}", clerkId);
        userService.deactivateUser(clerkId);
    }
}

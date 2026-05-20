package ma.smartfleet.backend.service;

import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.model.Client;
import ma.smartfleet.backend.model.Driver;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    private void push(String email, String title, String body) {
        log.info(">>> PUSH [{}] title='{}' body='{}'", email, title, body);
        /*
         * FCM integration example (when Firebase Admin SDK is configured):
         *
         * String token = getDeviceToken(email);
         * Message msg = Message.builder()
         *     .setToken(token)
         *     .setNotification(Notification.builder().setTitle(title).setBody(body).build())
         *     .build();
         * FirebaseMessaging.getInstance().send(msg);
         */
    }

    public void notifyDriverAboutRoute(Driver driver, String title, String body) {
        push(driver.getEmail(), title, body);
    }

    public void notifyClientArrivingSoon(Client client, String driverName, int estimatedMinutes) {
        push(client.getEmail(),
                "Votre livraison arrive !",
                String.format("Votre livreur %s arrivera dans environ %d minutes.", driverName, estimatedMinutes));
    }

    public void notifyClientOrderUpdate(Client client, Long orderId, String status) {
        push(client.getEmail(),
                "Mise à jour commande",
                String.format("Commande #%d : %s", orderId, status));
    }

    public void notifyDriverProgramComplete(Driver driver, Long subProgramId) {
        push(driver.getEmail(),
                "Fin de programme",
                String.format("Bravo ! Programme #%d terminé.", subProgramId));
    }
}

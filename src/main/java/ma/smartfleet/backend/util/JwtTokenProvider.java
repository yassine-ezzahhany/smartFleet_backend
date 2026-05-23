package ma.smartfleet.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import ma.smartfleet.backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey jwtSecretKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms}") long expirationMs) {
        // Ensure secret is long enough for HMAC-SHA256 (at least 256 bits / 32 bytes)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("JWT secret is too short. Generating a secure key instead.");
            this.jwtSecretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            this.jwtSecretKey = Keys.hmacShaKeyFor(keyBytes);
        }
        this.jwtExpirationMs = expirationMs;
    }

    /**
     * Génère un token JWT à partir d'un utilisateur.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSecretKey)
                .compact();
    }

    /**
     * Extrait l'email de l'utilisateur à partir du token JWT.
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Valide le token JWT.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Token JWT invalide : {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Token JWT expiré : {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT non supporté : {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("La chaîne de revendications JWT est vide : {}", e.getMessage());
        } catch (JwtException e) {
            log.error("Erreur de signature ou de validation JWT : {}", e.getMessage());
        }
        return false;
    }
}

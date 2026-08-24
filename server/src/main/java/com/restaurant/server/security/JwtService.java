package com.restaurant.server.security;

import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Stateless JWT issuer/verifier. HS256 with a server-generated secret. Secret is stored in
 * <configDir>/jwt.key (Base64) and persisted on first boot by JwtSecretBootstrap (PHASE 3 bootstrap).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final RestaurantProperties props;
    private final SecretKey key;

    public JwtService(RestaurantProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(loadOrCreateSecret(props).getBytes(StandardCharsets.UTF_8));
    }

    private String loadOrCreateSecret(RestaurantProperties props) {
        String keyFile = props.getConfigDir() + "/jwt.key";
        java.io.File f = new java.io.File(keyFile);
        if (f.exists() && f.length() > 0) {
            try {
                return java.nio.file.Files.readString(f.toPath(), StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot read jwt.key", e);
            }
        }
        try {
            java.nio.file.Files.createDirectories(f.getParentFile().toPath());
        } catch (Exception ignored) {}
        byte[] secret = new byte[64];
        new SecureRandom().nextBytes(secret);
        String b64 = Base64.getEncoder().encodeToString(secret);
        try {
            java.nio.file.Files.writeString(f.toPath(), b64, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to persist jwt.key (will be regenerated on restart): {}", e.getMessage());
        }
        return b64;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getJwt().getExpirationDuration());
        return Jwts.builder()
                .issuer(props.getJwt().getIssuer())
                .subject(String.valueOf(user.getId()))
                .claims(Map.of(
                        "username", user.getUsername(),
                        "role", user.getRole().name(),
                        "lang", user.getLang() == null ? "vi" : user.getLang()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(props.getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = Long.parseLong(c.getSubject());
            String username = c.get("username", String.class);
            String role = c.get("role", String.class);
            String lang = c.get("lang", String.class);
            if (lang == null) lang = "vi";
            return Optional.of(new AuthPrincipal(id, username, role, lang));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record AuthPrincipal(Long userId, String username, String role, String lang) {}
}
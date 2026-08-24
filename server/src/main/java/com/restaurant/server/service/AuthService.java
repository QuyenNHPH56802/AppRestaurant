package com.restaurant.server.service;

import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.dto.AuthDtos;
import com.restaurant.server.entity.AuditLog;
import com.restaurant.server.entity.User;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import com.restaurant.server.repository.AuditLogRepository;
import com.restaurant.server.repository.UserRepository;
import com.restaurant.server.security.JwtService;
import com.restaurant.server.security.LoginRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginRateLimiter rateLimiter;
    private final AuditLogRepository auditLogs;
    private final MessageService messages;
    private final RestaurantProperties props;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                       LoginRateLimiter rateLimiter, AuditLogRepository auditLogs,
                       MessageService messages, RestaurantProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.rateLimiter = rateLimiter;
        this.auditLogs = auditLogs;
        this.messages = messages;
        this.props = props;
    }

    @Transactional
    public AuthDtos.LoginResponse login(String username, String password, String clientIp) {
        String rateKey = (username == null ? "_" : username) + "|" + (clientIp == null ? "_" : clientIp);
        if (!rateLimiter.isAllowed(rateKey)) {
            audit("LOGIN_RATE_LIMITED", username, null, null);
            throw AppException.tooManyRequests(messages.get("auth.login.rate_limited"));
        }

        User u = users.findByUsername(username).orElse(null);
        if (u == null || !encoder.matches(password, u.getPasswordHash())) {
            rateLimiter.recordFailed(rateKey);
            audit("LOGIN_FAILED", username, null, null);
            log.info("login failed for username={} ip={}", username, clientIp);
            throw AppException.unauthorized(messages.get("auth.login.invalid_credentials"));
        }
        if (u.getStatus() == User.Status.DISABLED) {
            rateLimiter.recordFailed(rateKey);
            audit("LOGIN_DISABLED", username, u.getId(), null);
            throw AppException.forbidden(messages.get("auth.login.user_disabled"));
        }

        rateLimiter.clear(rateKey);
        audit("LOGIN_SUCCESS", username, u.getId(), null);

        String token = jwt.issue(u);
        long expiresInSeconds = props.getJwt().getExpirationDuration().toSeconds();
        return new AuthDtos.LoginResponse(
            token,
            expiresInSeconds,
            new AuthDtos.UserSummary(u.getId(), u.getUsername(), u.getFullName(),
                    u.getRole().name(), u.getLang())
        );
    }

    @Transactional
    public AuthDtos.LogoutResponse logout(Long userId, String username) {
        // Stateless: client must delete the token. We log the event.
        audit("LOGOUT", username, userId, null);
        return new AuthDtos.LogoutResponse(messages.get("auth.logout.success"));
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserSummary me(Long userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> AppException.unauthorized(messages.get("auth.me.required")));
        if (u.getStatus() == User.Status.DISABLED) {
            throw AppException.forbidden(messages.get("auth.login.user_disabled"));
        }
        return new AuthDtos.UserSummary(u.getId(), u.getUsername(), u.getFullName(),
                u.getRole().name(), u.getLang());
    }

    private void audit(String action, String username, Long userId, String details) {
        try {
            AuditLog a = new AuditLog();
            a.setAction(action);
            a.setEntity("user");
            a.setEntityId(username);
            a.setUserId(userId);
            a.setDetails(details);
            auditLogs.save(a);
        } catch (Exception e) {
            log.warn("audit log write failed: {}", e.getMessage());
        }
    }
}
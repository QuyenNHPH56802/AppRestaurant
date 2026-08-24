package com.restaurant.server.bootstrap;

import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.entity.User;
import com.restaurant.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PHASE 3 bootstrap. Runs once on startup, before the API starts accepting traffic.
 * - Ensures all data directories exist.
 * - If the admin / staff seed users still carry the SQL placeholder hash, replaces it
 *   with a real BCrypt hash so default credentials work.
 * - Idempotent: no destructive changes on subsequent runs.
 */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final String PLACEHOLDER = "__";

    private final RestaurantProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrap(RestaurantProperties props, UserRepository users, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureDirectories();
        ensureSeedUser("admin", "Quản trị viên", User.Role.ADMIN, "admin123");
        ensureSeedUser("nhanvien01", "Nhân viên 01", User.Role.STAFF, "staff123");
        log.info("AdminBootstrap complete: data-dir={} users={}", props.getDataDir(), users.count());
    }

    private void ensureDirectories() {
        for (String dir : new String[]{props.getDataDir(), props.getUploadsDir(),
                props.getBackupsDir(), props.getLogsDir(), props.getConfigDir()}) {
            try {
                Path p = Paths.get(dir);
                Files.createDirectories(p);
            } catch (Exception e) {
                log.warn("Failed to create directory {}: {}", dir, e.getMessage());
            }
        }
    }

    private void ensureSeedUser(String username, String fullName, User.Role role, String defaultPassword) {
        users.findByUsername(username).ifPresentOrElse(u -> {
            String hash = u.getPasswordHash();
            if (hash == null || hash.startsWith(PLACEHOLDER)) {
                u.setPasswordHash(encoder.encode(defaultPassword));
                users.save(u);
                log.info("Replaced placeholder password hash for user '{}'", username);
            }
            if (u.getStatus() != User.Status.ACTIVE) {
                u.setStatus(User.Status.ACTIVE);
                users.save(u);
            }
        }, () -> {
            User u = new User();
            u.setUsername(username);
            u.setFullName(fullName);
            u.setRole(role);
            u.setStatus(User.Status.ACTIVE);
            u.setLang("vi");
            u.setPasswordHash(encoder.encode(defaultPassword));
            users.save(u);
            log.info("Created seed user '{}' ({}).", username, role);
        });
    }
}
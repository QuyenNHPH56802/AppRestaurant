package com.restaurant.server.service;

import com.restaurant.server.dto.AdminDtos;
import com.restaurant.server.entity.AuditLog;
import com.restaurant.server.entity.User;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import com.restaurant.server.repository.AuditLogRepository;
import com.restaurant.server.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final MessageService messages;
    private final AuditLogRepository auditLogs;

    public AdminUserService(UserRepository users, PasswordEncoder encoder,
                            MessageService messages, AuditLogRepository auditLogs) {
        this.users = users;
        this.encoder = encoder;
        this.messages = messages;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public Page<AdminDtos.UserView> list(int page, int size) {
        return users.findAll(PageRequest.of(page, size, Sort.by("id").ascending()))
                .map(this::toView);
    }

    @Transactional
    public AdminDtos.UserView create(AdminDtos.UserRequest req, Long actorId) {
        if (users.existsByUsername(req.username())) {
            throw AppException.conflict("USER_DUPLICATE", messages.get("user.duplicate"));
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setFullName(req.fullName());
        u.setRole(parseRole(req.role()));
        u.setStatus(parseStatus(req.status()));
        u.setLang("ko".equalsIgnoreCase(req.lang()) ? "ko" : "vi");
        users.save(u);
        audit("USER_CREATE", u.getId(), actorId, u.getUsername());
        return toView(u);
    }

    @Transactional
    public AdminDtos.UserView update(Long id, AdminDtos.UserRequest req, Long actorId) {
        User u = users.findById(id)
                .orElseThrow(() -> AppException.notFound(messages.get("user.not_found")));
        u.setFullName(req.fullName());
        u.setRole(parseRole(req.role()));
        u.setStatus(parseStatus(req.status()));
        u.setLang("ko".equalsIgnoreCase(req.lang()) ? "ko" : "vi");
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(encoder.encode(req.password()));
        }
        users.save(u);
        audit("USER_UPDATE", id, actorId, u.getUsername());
        return toView(u);
    }

    @Transactional
    public AdminDtos.UserView patch(Long id, AdminDtos.UserPatchRequest req, Long actorId) {
        User u = users.findById(id)
                .orElseThrow(() -> AppException.notFound(messages.get("user.not_found")));
        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.role() != null) u.setRole(parseRole(req.role()));
        if (req.status() != null) u.setStatus(parseStatus(req.status()));
        if (req.lang() != null) u.setLang("ko".equalsIgnoreCase(req.lang()) ? "ko" : "vi");
        users.save(u);
        audit("USER_PATCH", id, actorId, u.getUsername());
        return toView(u);
    }

    @Transactional
    public AdminDtos.UserView resetPassword(Long id, String newPassword, Long actorId) {
        User u = users.findById(id)
                .orElseThrow(() -> AppException.notFound(messages.get("user.not_found")));
        u.setPasswordHash(encoder.encode(newPassword));
        users.save(u);
        audit("USER_RESET_PASSWORD", id, actorId, u.getUsername());
        return toView(u);
    }

    private AdminDtos.UserView toView(User u) {
        return new AdminDtos.UserView(u.getId(), u.getUsername(), u.getFullName(),
                u.getRole().name(), u.getStatus().name(), u.getLang());
    }

    private User.Role parseRole(String s) {
        try { return User.Role.valueOf(s); }
        catch (Exception e) { throw AppException.badRequest("INVALID_ROLE", "Invalid role: " + s); }
    }

    private User.Status parseStatus(String s) {
        try { return User.Status.valueOf(s); }
        catch (Exception e) { throw AppException.badRequest("INVALID_STATUS", "Invalid status: " + s); }
    }

    private void audit(String action, Long entityId, Long actorId, String details) {
        try {
            AuditLog a = new AuditLog();
            a.setAction(action);
            a.setEntity("user");
            a.setEntityId(entityId == null ? null : entityId.toString());
            a.setUserId(actorId);
            a.setDetails(details);
            auditLogs.save(a);
        } catch (Exception ignored) {}
    }
}
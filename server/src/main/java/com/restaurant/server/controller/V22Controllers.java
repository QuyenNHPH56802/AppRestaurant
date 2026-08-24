package com.restaurant.server.controller;

import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.V22Dtos;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.security.JwtAuthFilter;
import com.restaurant.server.service.V22Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * V2.2 — REST controllers for shifts, zones, checklists, check-ins and
 * activity logs.
 *
 * <p>Endpoints are split by audience:</p>
 * <ul>
 *     <li>{@code /api/admin/*} — admin only ({@code hasRole('ADMIN')}).</li>
 *     <li>{@code /api/me/*} — any authenticated employee.</li>
 * </ul>
 */
public class V22Controllers {

    private V22Controllers() {}

    private static Long requireUserId(JwtAuthFilter.AuthenticatedUser principal) {
        if (principal == null) throw AppException.unauthorized("Authentication required");
        return principal.id();
    }

    // ============================ Shifts (admin) ============================

    @RestController
    @RequestMapping("/api/admin/shifts")
    @PreAuthorize("hasRole('ADMIN')")
    public static class AdminShiftController {

        private final V22Service service;

        public AdminShiftController(V22Service service) { this.service = service; }

        @GetMapping
        public ResponseEntity<ApiResponse<List<V22Dtos.ShiftView>>> list() {
            return ResponseEntity.ok(ApiResponse.ok(service.listShifts()));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<V22Dtos.ShiftView>> create(
                @Valid @RequestBody V22Dtos.ShiftRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.createShift(req, principal == null ? null : principal.id())));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<V22Dtos.ShiftView>> update(
                @PathVariable Long id,
                @Valid @RequestBody V22Dtos.ShiftRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.updateShift(id, req, principal == null ? null : principal.id())));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> delete(
                @PathVariable Long id,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            service.deleteShift(id, principal == null ? null : principal.id());
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
    }

    // ============================ Shift assignments (admin) ============================

    @RestController
    @RequestMapping("/api/admin/shift-assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public static class AdminShiftAssignmentController {

        private final V22Service service;

        public AdminShiftAssignmentController(V22Service service) { this.service = service; }

        @GetMapping
        public ResponseEntity<ApiResponse<List<V22Dtos.ShiftAssignmentView>>> listByDate(
                @RequestParam String date) {
            return ResponseEntity.ok(ApiResponse.ok(service.listAssignmentsByDate(date)));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<V22Dtos.ShiftAssignmentView>> create(
                @Valid @RequestBody V22Dtos.ShiftAssignmentRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.createAssignment(req, principal == null ? null : principal.id())));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<V22Dtos.ShiftAssignmentView>> update(
                @PathVariable Long id,
                @Valid @RequestBody V22Dtos.ShiftAssignmentRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.updateAssignment(id, req, principal == null ? null : principal.id())));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> delete(
                @PathVariable Long id,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            service.deleteAssignment(id, principal == null ? null : principal.id());
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
    }

    // ============================ Shift assignments (me) ============================

    @RestController
    @RequestMapping("/api/me/shifts")
    public static class MeShiftController {

        private final V22Service service;

        public MeShiftController(V22Service service) { this.service = service; }

        @GetMapping
        public ResponseEntity<ApiResponse<List<V22Dtos.ShiftAssignmentView>>> myShifts(
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            Long userId = requireUserId(principal);
            return ResponseEntity.ok(ApiResponse.ok(service.listAssignmentsForUser(userId)));
        }

        @PostMapping("/{id}/respond")
        public ResponseEntity<ApiResponse<V22Dtos.ShiftAssignmentView>> respond(
                @PathVariable Long id,
                @Valid @RequestBody V22Dtos.ShiftAssignmentRespondRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            Long userId = requireUserId(principal);
            return ResponseEntity.ok(ApiResponse.ok(service.respondAssignment(userId, id, req)));
        }
    }

    // ============================ Zones (admin) ============================

    @RestController
    @RequestMapping("/api/admin/zones")
    @PreAuthorize("hasRole('ADMIN')")
    public static class AdminZoneController {

        private final V22Service service;

        public AdminZoneController(V22Service service) { this.service = service; }

        @GetMapping
        public ResponseEntity<ApiResponse<List<V22Dtos.ZoneView>>> list() {
            return ResponseEntity.ok(ApiResponse.ok(service.listZones(false, null)));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<V22Dtos.ZoneView>> create(
                @Valid @RequestBody V22Dtos.ZoneRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.createZone(req, principal == null ? null : principal.id())));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<V22Dtos.ZoneView>> update(
                @PathVariable Long id,
                @Valid @RequestBody V22Dtos.ZoneRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.updateZone(id, req, principal == null ? null : principal.id())));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> delete(
                @PathVariable Long id,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            service.deleteZone(id, principal == null ? null : principal.id());
            return ResponseEntity.ok(ApiResponse.ok(null));
        }

        /**
         * Regenerate the QR token for a zone. Useful when the printed QR is
         * lost or compromised. Returns the new token in plaintext (the old
         * one is invalidated).
         */
        @PostMapping("/{id}/qr/regenerate")
        public ResponseEntity<ApiResponse<V22Dtos.ZoneQrTokenView>> regenerateQr(
                @PathVariable Long id,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            String token = service.regenerateQrToken(id, principal == null ? null : principal.id());
            return ResponseEntity.ok(ApiResponse.ok(new V22Dtos.ZoneQrTokenView(id, token)));
        }

        @GetMapping("/{id}/current")
        public ResponseEntity<ApiResponse<List<V22Dtos.ZoneAssignmentView>>> current(
                @PathVariable Long id) {
            return ResponseEntity.ok(ApiResponse.ok(service.currentByZone(id)));
        }
    }

    // ============================ Zones (me) ============================

    @RestController
    @RequestMapping("/api/me/zones")
    public static class MeZoneController {

        private final V22Service service;

        public MeZoneController(V22Service service) { this.service = service; }

        @GetMapping
        public ResponseEntity<ApiResponse<List<V22Dtos.ZoneView>>> list(
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            Long userId = principal == null ? null : principal.id();
            return ResponseEntity.ok(ApiResponse.ok(service.listZones(true, userId)));
        }

        @GetMapping("/current")
        public ResponseEntity<ApiResponse<V22Dtos.ZoneAssignmentView>> current(
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(service.currentZone(requireUserId(principal))));
        }

        @GetMapping("/history")
        public ResponseEntity<ApiResponse<List<V22Dtos.ZoneAssignmentView>>> history(
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(service.zoneAssignmentsForUser(requireUserId(principal))));
        }

        @PostMapping("/assign")
        public ResponseEntity<ApiResponse<V22Dtos.ZoneAssignmentView>> assign(
                @Valid @RequestBody V22Dtos.ZoneAssignRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            // Self-service: user can only move themselves.
            Long meId = requireUserId(principal);
            if (!meId.equals(req.userId())) {
                throw AppException.forbidden("Cannot assign another user to a zone");
            }
            return ResponseEntity.ok(ApiResponse.ok(
                    service.assignUser(req, principal.id(), true)));
        }
    }

    // ============================ Zone assignments (admin) ============================

    @RestController
    @RequestMapping("/api/admin/zone-assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public static class AdminZoneAssignmentController {

        private final V22Service service;

        public AdminZoneAssignmentController(V22Service service) { this.service = service; }

        @PostMapping
        public ResponseEntity<ApiResponse<V22Dtos.ZoneAssignmentView>> assign(
                @Valid @RequestBody V22Dtos.ZoneAssignRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.assignUser(req, principal.id(), false)));
        }

        @GetMapping("/user/{userId}")
        public ResponseEntity<ApiResponse<List<V22Dtos.ZoneAssignmentView>>> forUser(
                @PathVariable Long userId) {
            return ResponseEntity.ok(ApiResponse.ok(service.zoneAssignmentsForUser(userId)));
        }
    }

    // ============================ Checklists (admin) ============================

    @RestController
    @RequestMapping("/api/admin/checklists")
    @PreAuthorize("hasRole('ADMIN')")
    public static class AdminChecklistController {

        private final V22Service service;

        public AdminChecklistController(V22Service service) { this.service = service; }

        @GetMapping
        public ResponseEntity<ApiResponse<List<V22Dtos.ChecklistView>>> list(
                @RequestParam(required = false) Long zoneId) {
            return ResponseEntity.ok(ApiResponse.ok(service.listChecklists(zoneId, false)));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<V22Dtos.ChecklistView>> create(
                @Valid @RequestBody V22Dtos.ChecklistRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.createChecklist(req, principal == null ? null : principal.id())));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<V22Dtos.ChecklistView>> update(
                @PathVariable Long id,
                @Valid @RequestBody V22Dtos.ChecklistRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.updateChecklist(id, req, principal == null ? null : principal.id())));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> delete(
                @PathVariable Long id,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            service.deleteChecklist(id, principal == null ? null : principal.id());
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
    }

    // ============================ Checklists (me) ============================

    @RestController
    @RequestMapping("/api/me/checklists")
    public static class MeChecklistController {

        private final V22Service service;

        public MeChecklistController(V22Service service) { this.service = service; }

        @GetMapping
        public ResponseEntity<ApiResponse<List<V22Dtos.ChecklistView>>> list(
                @RequestParam(required = false) Long zoneId) {
            return ResponseEntity.ok(ApiResponse.ok(service.listChecklists(zoneId, true)));
        }

        @PostMapping("/complete")
        public ResponseEntity<ApiResponse<V22Dtos.ChecklistCompletionView>> complete(
                @Valid @RequestBody V22Dtos.ChecklistCompleteRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.completeTask(requireUserId(principal), req)));
        }

        @GetMapping("/completions")
        public ResponseEntity<ApiResponse<List<V22Dtos.ChecklistCompletionView>>> recent(
                @RequestParam(defaultValue = "20") int limit,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.recentCompletions(requireUserId(principal), limit)));
        }
    }

    // ============================ Check-ins (me + admin) ============================

    @RestController
    @RequestMapping("/api/me/check-ins")
    public static class MeCheckInController {

        private final V22Service service;

        public MeCheckInController(V22Service service) { this.service = service; }

        @PostMapping
        public ResponseEntity<ApiResponse<V22Dtos.CheckInView>> checkIn(
                @Valid @RequestBody V22Dtos.CheckInRequest req,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal,
                HttpServletRequest request) {
            Long userId = requireUserId(principal);
            String ip = request == null ? null : request.getRemoteAddr();
            return ResponseEntity.ok(ApiResponse.ok(service.checkIn(userId, req, ip)));
        }

        @GetMapping
        public ResponseEntity<ApiResponse<List<V22Dtos.CheckInView>>> recent(
                @RequestParam(defaultValue = "20") int limit,
                @AuthenticationPrincipal JwtAuthFilter.AuthenticatedUser principal) {
            return ResponseEntity.ok(ApiResponse.ok(
                    service.recentCheckIns(requireUserId(principal), limit)));
        }
    }

    /** Phase G — admin browse of all check-ins. */
    @RestController
    @RequestMapping("/api/admin/check-ins")
    @PreAuthorize("hasRole('ADMIN')")
    public static class AdminCheckInController {

        private final V22Service service;

        public AdminCheckInController(V22Service service) { this.service = service; }

        @GetMapping
        public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> list(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(required = false) Long userId,
                @RequestParam(required = false) Long zoneId,
                @RequestParam(required = false) String action) {
            int safeSize = Math.max(1, Math.min(size, 200));
            java.util.List<com.restaurant.server.entity.CheckInLog> rows;
            if (userId != null) {
                rows = service.checkInRepo().findAllByUserIdOrderByCreatedAtDesc(userId);
            } else if (zoneId != null) {
                rows = service.checkInRepo().findAllByZoneIdOrderByCreatedAtDesc(zoneId);
            } else {
                rows = service.checkInRepo().findAllByOrderByCreatedAtDesc(
                        org.springframework.data.domain.PageRequest.of(page, safeSize));
            }
            if (action != null && !action.isBlank()) {
                rows = rows.stream()
                        .filter(r -> action.equalsIgnoreCase(r.getAction() == null ? "" : r.getAction().name()))
                        .toList();
            }
            java.util.List<V22Dtos.CheckInView> views = service.toCheckInViews(rows);
            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("items", views);
            resp.put("page", page);
            resp.put("size", safeSize);
            resp.put("total", service.checkInRepo().count());
            return ResponseEntity.ok(ApiResponse.ok(resp));
        }
    }

    // ============================ Activity logs (admin) ============================

    @RestController
    @RequestMapping("/api/admin/activity-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public static class AdminActivityLogController {

        private final V22Service service;

        public AdminActivityLogController(V22Service service) { this.service = service; }

        @GetMapping
        public ResponseEntity<ApiResponse<List<V22Dtos.ActivityLogView>>> list(
                @RequestParam(defaultValue = "50") int limit,
                @RequestParam(required = false) String action,
                @RequestParam(required = false) String entity) {
            return ResponseEntity.ok(ApiResponse.ok(service.recentActivity(limit, action, entity)));
        }
    }
}

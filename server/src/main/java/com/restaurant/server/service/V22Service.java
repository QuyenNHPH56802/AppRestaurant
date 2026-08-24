package com.restaurant.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.server.dto.V22Dtos;
import com.restaurant.server.entity.ActivityLog;
import com.restaurant.server.entity.AuditLog;
import com.restaurant.server.entity.CheckInLog;
import com.restaurant.server.entity.Checklist;
import com.restaurant.server.entity.ChecklistCompletion;
import com.restaurant.server.entity.ChecklistTask;
import com.restaurant.server.entity.ChecklistTaskTranslation;
import com.restaurant.server.entity.ChecklistTranslation;
import com.restaurant.server.entity.Shift;
import com.restaurant.server.entity.ShiftAssignment;
import com.restaurant.server.entity.User;
import com.restaurant.server.entity.Zone;
import com.restaurant.server.entity.ZoneAssignment;
import com.restaurant.server.entity.ZoneTranslation;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import com.restaurant.server.repository.ActivityLogRepository;
import com.restaurant.server.repository.AuditLogRepository;
import com.restaurant.server.repository.CheckInLogRepository;
import com.restaurant.server.repository.ChecklistCompletionRepository;
import com.restaurant.server.repository.ChecklistRepository;
import com.restaurant.server.repository.ChecklistTaskRepository;
import com.restaurant.server.repository.ShiftAssignmentRepository;
import com.restaurant.server.repository.ShiftRepository;
import com.restaurant.server.repository.UserRepository;
import com.restaurant.server.repository.ZoneAssignmentRepository;
import com.restaurant.server.repository.ZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * V2.2 — business logic for shifts, zones, checklists, check-ins, activity logs.
 *
 * <p>This is the unified service for both admin and staff endpoints. It
 * deliberately mixes the two so the controllers stay thin; access control is
 * enforced by the controller annotations.</p>
 *
 * <p>Side-effects (notifications, audit logs) live in this service so callers
 * cannot accidentally skip them.</p>
 */
@Service
public class V22Service {

    private static final Logger log = LoggerFactory.getLogger(V22Service.class);

    private final ShiftRepository shifts;
    private final ShiftAssignmentRepository shiftAssignments;
    private final ZoneRepository zones;
    private final ZoneAssignmentRepository zoneAssignments;
    private final ChecklistRepository checklists;
    private final ChecklistTaskRepository checklistTasks;
    private final ChecklistCompletionRepository checklistCompletions;
    private final CheckInLogRepository checkInLogs;
    private final ActivityLogRepository activityLogs;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;
    private final NotificationService notifications;
    private final MessageService messages;
    private final ObjectMapper objectMapper;

    public V22Service(ShiftRepository shifts,
                      ShiftAssignmentRepository shiftAssignments,
                      ZoneRepository zones,
                      ZoneAssignmentRepository zoneAssignments,
                      ChecklistRepository checklists,
                      ChecklistTaskRepository checklistTasks,
                      ChecklistCompletionRepository checklistCompletions,
                      CheckInLogRepository checkInLogs,
                      ActivityLogRepository activityLogs,
                      UserRepository users,
                      AuditLogRepository auditLogs,
                      NotificationService notifications,
                      MessageService messages,
                      ObjectMapper objectMapper) {
        this.shifts = shifts;
        this.shiftAssignments = shiftAssignments;
        this.zones = zones;
        this.zoneAssignments = zoneAssignments;
        this.checklists = checklists;
        this.checklistTasks = checklistTasks;
        this.checklistCompletions = checklistCompletions;
        this.checkInLogs = checkInLogs;
        this.activityLogs = activityLogs;
        this.users = users;
        this.auditLogs = auditLogs;
        this.notifications = notifications;
        this.messages = messages;
        this.objectMapper = objectMapper;
    }

    // ====================== Shifts ======================

    @Transactional(readOnly = true)
    public List<V22Dtos.ShiftView> listShifts() {
        return shifts.findAllByOrderBySortOrderAsc().stream()
                .map(V22Dtos.ShiftView::from)
                .toList();
    }

    @Transactional
    public V22Dtos.ShiftView createShift(V22Dtos.ShiftRequest req, Long actorId) {
        if (req.startTime().equals(req.endTime())) {
            throw AppException.badRequest("INVALID_SHIFT", "startTime equals endTime");
        }
        Shift s = new Shift();
        applyShiftFields(s, req);
        Shift saved = shifts.save(s);
        audit("SHIFT_CREATE", saved.getId(), actorId, null);
        return V22Dtos.ShiftView.from(saved);
    }

    @Transactional
    public V22Dtos.ShiftView updateShift(Long id, V22Dtos.ShiftRequest req, Long actorId) {
        Shift s = shifts.findById(id)
                .orElseThrow(() -> AppException.notFound(messages.get("shift.not_found")));
        applyShiftFields(s, req);
        Shift saved = shifts.save(s);
        audit("SHIFT_UPDATE", saved.getId(), actorId, null);
        return V22Dtos.ShiftView.from(saved);
    }

    @Transactional
    public void deleteShift(Long id, Long actorId) {
        Shift s = shifts.findById(id)
                .orElseThrow(() -> AppException.notFound(messages.get("shift.not_found")));
        // Soft-disable rather than hard delete so existing assignments stay valid.
        s.setIsActive(0);
        shifts.save(s);
        audit("SHIFT_DISABLE", id, actorId, null);
    }

    // ====================== Shift assignments ======================

    @Transactional(readOnly = true)
    public List<V22Dtos.ShiftAssignmentView> listAssignmentsForUser(Long userId) {
        User u = users.findById(userId).orElse(null);
        String userName = u == null ? null : fullName(u);
        List<ShiftAssignment> rows = shiftAssignments.findAllByUserIdOrderByDateDesc(userId);
        Map<Long, Shift> cache = new HashMap<>();
        return rows.stream().map(a -> {
            Shift s = cache.computeIfAbsent(a.getShiftId(), id -> shifts.findById(id).orElse(null));
            return V22Dtos.ShiftAssignmentView.from(a, s, userName);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<V22Dtos.ShiftAssignmentView> listAssignmentsByDate(String date) {
        List<ShiftAssignment> rows = shiftAssignments.findAll().stream()
                .filter(a -> date.equals(a.getDate()))
                .sorted((x, y) -> Long.compare(
                        x.getShiftId() == null ? 0 : x.getShiftId(),
                        y.getShiftId() == null ? 0 : y.getShiftId()))
                .toList();
        Map<Long, User> userCache = new HashMap<>();
        Map<Long, Shift> shiftCache = new HashMap<>();
        return rows.stream().map(a -> {
            Shift s = shiftCache.computeIfAbsent(a.getShiftId(), id -> shifts.findById(id).orElse(null));
            User u = userCache.computeIfAbsent(a.getUserId(), id -> users.findById(id).orElse(null));
            return V22Dtos.ShiftAssignmentView.from(a, s, u == null ? null : fullName(u));
        }).toList();
    }

    @Transactional
    public V22Dtos.ShiftAssignmentView createAssignment(V22Dtos.ShiftAssignmentRequest req, Long actorId) {
        if (!shifts.findById(req.shiftId()).isPresent()) {
            throw AppException.notFound(messages.get("shift.not_found"));
        }
        User u = users.findById(req.userId())
                .orElseThrow(() -> AppException.notFound(messages.get("user.not_found")));
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftId(req.shiftId());
        a.setUserId(req.userId());
        a.setDate(req.date());
        a.setNotes(req.notes());
        if (req.status() != null && !req.status().isBlank()) {
            a.setStatus(parseStatus(req.status()));
        }
        ShiftAssignment saved = shiftAssignments.save(a);
        activity("SHIFT_ASSIGN", "shift_assignment", saved.getId().toString(),
                actorId, req.userId(), payloadOf("shiftId", req.shiftId(), "date", req.date()));
        notifyShiftAssignment(saved, u, "SHIFT_ASSIGNED",
                "Bạn vừa được phân ca làm việc",
                "새 근무 배정이 등록되었습니다");
        audit("SHIFT_ASSIGN_CREATE", saved.getId(), actorId, req.status());
        return V22Dtos.ShiftAssignmentView.from(saved, shifts.findById(saved.getShiftId()).orElse(null), fullName(u));
    }

    @Transactional
    public V22Dtos.ShiftAssignmentView updateAssignment(Long id, V22Dtos.ShiftAssignmentRequest req, Long actorId) {
        ShiftAssignment a = shiftAssignments.findById(id)
                .orElseThrow(() -> AppException.notFound("Shift assignment not found"));
        if (req.shiftId() != null) a.setShiftId(req.shiftId());
        if (req.userId() != null) a.setUserId(req.userId());
        if (req.date() != null && !req.date().isBlank()) a.setDate(req.date());
        if (req.notes() != null) a.setNotes(req.notes());
        if (req.status() != null && !req.status().isBlank()) {
            a.setStatus(parseStatus(req.status()));
        }
        ShiftAssignment saved = shiftAssignments.save(a);
        User u = users.findById(saved.getUserId()).orElse(null);
        Shift s = shifts.findById(saved.getShiftId()).orElse(null);
        audit("SHIFT_ASSIGN_UPDATE", saved.getId(), actorId, saved.getStatus().name());
        return V22Dtos.ShiftAssignmentView.from(saved, s, u == null ? null : fullName(u));
    }

    @Transactional
    public void deleteAssignment(Long id, Long actorId) {
        ShiftAssignment a = shiftAssignments.findById(id)
                .orElseThrow(() -> AppException.notFound("Shift assignment not found"));
        shiftAssignments.delete(a);
        audit("SHIFT_ASSIGN_DELETE", id, actorId, null);
    }

    /**
     * Staff self-service: accept / reject / request change / cancel their own
     * assignment. Notifications go back to admins (so they can react).
     */
    @Transactional
    public V22Dtos.ShiftAssignmentView respondAssignment(Long userId,
                                                         Long assignmentId,
                                                         V22Dtos.ShiftAssignmentRespondRequest req) {
        ShiftAssignment a = shiftAssignments.findById(assignmentId)
                .orElseThrow(() -> AppException.notFound("Shift assignment not found"));
        if (!userId.equals(a.getUserId())) {
            // Don't leak existence
            throw AppException.notFound("Shift assignment not found");
        }
        ShiftAssignment.Status target = parseStatus(req.status());
        // Legal transitions for staff:
        //   SCHEDULED/CONFIRMED -> ACCEPTED | REJECTED | CHANGE_REQUESTED | CANCELLED
        // Staff cannot move the assignment out of a terminal state
        // (ACCEPTED, COMPLETED, CANCELLED, SWAPPED, REJECTED).
        if (a.getStatus() == ShiftAssignment.Status.ACCEPTED
                || a.getStatus() == ShiftAssignment.Status.COMPLETED
                || a.getStatus() == ShiftAssignment.Status.CANCELLED
                || a.getStatus() == ShiftAssignment.Status.SWAPPED
                || a.getStatus() == ShiftAssignment.Status.REJECTED) {
            throw AppException.conflict("INVALID_TRANSITION",
                    "Cannot transition from " + a.getStatus() + " by staff");
        }
        a.setStatus(target);
        if (req.notes() != null) a.setNotes(req.notes());
        ShiftAssignment saved = shiftAssignments.save(a);
        Shift s = shifts.findById(saved.getShiftId()).orElse(null);
        User u = users.findById(userId).orElse(null);
        String userName = u == null ? null : fullName(u);
        activity("SHIFT_ASSIGN_RESPOND", "shift_assignment", saved.getId().toString(),
                userId, null,
                payloadOf("status", saved.getStatus().name(), "notes", saved.getNotes() == null ? "" : saved.getNotes()));
        // Ping the admin pool so they can re-assign or note the rejection.
        notifyAdmins("SHIFT_RESPONSE",
                "Phản hồi ca từ " + (userName == null ? ("#" + userId) : userName),
                "직원 근무 응답: " + (userName == null ? ("#" + userId) : userName),
                Map.of("assignmentId", saved.getId().toString(),
                        "status", saved.getStatus().name()));
        return V22Dtos.ShiftAssignmentView.from(saved, s, userName);
    }

    // ====================== Zones ======================

    @Transactional(readOnly = true)
    public List<V22Dtos.ZoneView> listZones(boolean activeOnly, Long currentUserId) {
        List<Zone> rows = activeOnly
                ? zones.findAllByStatusOrderBySortOrderAsc(Zone.Status.ACTIVE)
                : zones.findAllByOrderBySortOrderAsc();
        Map<Long, ZoneAssignment> currentMap = currentUserId == null
                ? Map.of()
                : zoneAssignments.findFirstByUserIdAndIsCurrentOrderByEffectiveFromDesc(currentUserId, 1)
                .map(a -> Map.of(a.getZoneId(), a))
                .orElse(Map.of());
        return rows.stream().map(z -> {
            V22Dtos.ZoneView base = V22Dtos.ZoneView.from(z, z.getTranslations());
            return new V22Dtos.ZoneView(base.id(), base.code(), base.color(), base.status(),
                    base.sortOrder(), base.requiredStaff(),
                    currentMap.containsKey(base.id()),
                    base.translations());
        }).toList();
    }

    @Transactional
    public V22Dtos.ZoneView createZone(V22Dtos.ZoneRequest req, Long actorId) {
        if (zones.findByCode(req.code()).isPresent()) {
            throw AppException.conflict("ZONE_CODE_EXISTS", "Zone code already exists");
        }
        Zone z = new Zone();
        applyZoneFields(z, req);
        Zone saved = zones.save(z);
        audit("ZONE_CREATE", saved.getId(), actorId, null);
        return V22Dtos.ZoneView.from(saved, saved.getTranslations());
    }

    @Transactional
    public V22Dtos.ZoneView updateZone(Long id, V22Dtos.ZoneRequest req, Long actorId) {
        Zone z = zones.findById(id)
                .orElseThrow(() -> AppException.notFound("Zone not found"));
        Optional<Zone> existing = zones.findByCode(req.code());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw AppException.conflict("ZONE_CODE_EXISTS", "Zone code already exists");
        }
        // Mutate fields, then explicitly remove the old translations from the
        // DB before re-inserting the new ones. We avoid the JPA
        // orphanRemoval=true path here because cascading clears across a
        // managed entity can produce duplicate-key inserts on SQLite when the
        // orphan delete runs after the INSERT in the same transaction.
        z.setCode(req.code());
        z.setColor(req.color());
        z.setStatus(Zone.Status.valueOf(req.status()));
        z.setSortOrder(req.sortOrder());
        z.setRequiredStaff(req.requiredStaff());
        z.getTranslations().clear();
        zones.saveAndFlush(z);  // forces DELETE for old translations
        if (req.translations() != null) {
            for (V22Dtos.ZoneTranslationInput in : req.translations()) {
                ZoneTranslation t = new ZoneTranslation();
                t.setZone(z);
                t.setLanguageCode(in.lang());
                t.setName(in.name());
                t.setDescription(in.description());
                z.getTranslations().add(t);
            }
        }
        Zone saved = zones.save(z);
        audit("ZONE_UPDATE", saved.getId(), actorId, null);
        return V22Dtos.ZoneView.from(saved, saved.getTranslations());
    }

    @Transactional
    public void deleteZone(Long id, Long actorId) {
        Zone z = zones.findById(id)
                .orElseThrow(() -> AppException.notFound("Zone not found"));
        z.setStatus(Zone.Status.DISABLED);
        zones.save(z);
        audit("ZONE_DISABLE", id, actorId, null);
    }

    // ====================== Zone assignments ======================

    @Transactional(readOnly = true)
    public V22Dtos.ZoneAssignmentView currentZone(Long userId) {
        ZoneAssignment a = zoneAssignments
                .findFirstByUserIdAndIsCurrentOrderByEffectiveFromDesc(userId, 1)
                .orElse(null);
        if (a == null) return null;
        Zone z = zones.findById(a.getZoneId()).orElse(null);
        User u = users.findById(userId).orElse(null);
        return V22Dtos.ZoneAssignmentView.from(a, u == null ? null : fullName(u), z);
    }

    @Transactional(readOnly = true)
    public List<V22Dtos.ZoneAssignmentView> zoneAssignmentsForUser(Long userId) {
        User u = users.findById(userId).orElse(null);
        String userName = u == null ? null : fullName(u);
        return zoneAssignments.findAllByUserIdOrderByEffectiveFromDesc(userId).stream()
                .map(a -> {
                    Zone z = zones.findById(a.getZoneId()).orElse(null);
                    return V22Dtos.ZoneAssignmentView.from(a, userName, z);
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<V22Dtos.ZoneAssignmentView> currentByZone(Long zoneId) {
        Zone z = zones.findById(zoneId).orElse(null);
        return zoneAssignments.findAllByZoneIdAndIsCurrent(zoneId, 1).stream()
                .map(a -> {
                    User u = users.findById(a.getUserId()).orElse(null);
                    return V22Dtos.ZoneAssignmentView.from(a, u == null ? null : fullName(u), z);
                }).toList();
    }

    /**
     * Reassigns a user to a zone. The DB trigger closes the prior current
     * assignment automatically; we additionally notify the user (push + in-app)
     * so they know where to go next.
     */
    @Transactional
    public V22Dtos.ZoneAssignmentView assignUser(V22Dtos.ZoneAssignRequest req,
                                                 Long actorId,
                                                 boolean selfService) {
        Zone z = zones.findById(req.zoneId())
                .orElseThrow(() -> AppException.notFound("Zone not found"));
        if (z.getStatus() == Zone.Status.DISABLED) {
            throw AppException.conflict("ZONE_DISABLED", "Zone is disabled");
        }
        User u = users.findById(req.userId())
                .orElseThrow(() -> AppException.notFound("User not found"));
        ZoneAssignment prev = zoneAssignments
                .findFirstByUserIdAndIsCurrentOrderByEffectiveFromDesc(req.userId(), 1)
                .orElse(null);
        if (prev != null && prev.getZoneId().equals(req.zoneId())) {
            throw AppException.conflict("ALREADY_IN_ZONE", "User is already in this zone");
        }
        ZoneAssignment a = new ZoneAssignment();
        a.setUserId(req.userId());
        a.setZoneId(req.zoneId());
        a.setAssignedByUserId(actorId);
        a.setReason(req.reason());
        a.setIsCurrent(1);
        ZoneAssignment saved = zoneAssignments.save(a);
        activity("ZONE_ASSIGN", "zone_assignment", saved.getId().toString(),
                actorId, req.userId(),
                payloadOf("zoneId", req.zoneId(), "selfService", selfService));
        if (prev == null || !prev.getZoneId().equals(req.zoneId())) {
            notifyShiftAssignment2(req.userId(), u, "ZONE_CHANGED",
                    "Bạn được phân công sang khu vực mới",
                    "새 작업 구역으로 배치되었습니다",
                    Map.of("zoneId", req.zoneId().toString()));
        }
        audit("ZONE_ASSIGN", saved.getId(), actorId, req.reason());
        return V22Dtos.ZoneAssignmentView.from(saved, fullName(u), z);
    }

    // ====================== Checklists ======================

    @Transactional(readOnly = true)
    public List<V22Dtos.ChecklistView> listChecklists(Long zoneId, boolean activeOnly) {
        List<Checklist> rows;
        if (zoneId != null) {
            rows = activeOnly
                    ? checklists.findAllByZoneIdAndIsActiveOrderBySortOrderAsc(zoneId, 1)
                    : checklists.findAllByZoneIdOrderBySortOrderAsc(zoneId);
        } else {
            rows = activeOnly
                    ? checklists.findAllByIsActiveOrderBySortOrderAsc(1)
                    : checklists.findAll();
        }
        return rows.stream()
                .map(c -> V22Dtos.ChecklistView.from(c, c.getTranslations(), c.getTasks()))
                .toList();
    }

    @Transactional
    public V22Dtos.ChecklistView createChecklist(V22Dtos.ChecklistRequest req, Long actorId) {
        Zone z = zones.findById(req.zoneId())
                .orElseThrow(() -> AppException.notFound("Zone not found"));
        Checklist c = new Checklist();
        c.setZone(z);
        c.setIsActive(Boolean.TRUE.equals(req.active()) ? 1 : 0);
        c.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        applyChecklistTranslations(c, req.translations());
        applyChecklistTasks(c, req.tasks(), true);
        Checklist saved = checklists.save(c);
        audit("CHECKLIST_CREATE", saved.getId(), actorId, null);
        return V22Dtos.ChecklistView.from(saved, saved.getTranslations(), saved.getTasks());
    }

    @Transactional
    public V22Dtos.ChecklistView updateChecklist(Long id, V22Dtos.ChecklistRequest req, Long actorId) {
        Checklist c = checklists.findById(id)
                .orElseThrow(() -> AppException.notFound("Checklist not found"));
        if (req.zoneId() != null) {
            Zone z = zones.findById(req.zoneId())
                    .orElseThrow(() -> AppException.notFound("Zone not found"));
            c.setZone(z);
        }
        if (req.active() != null) c.setIsActive(Boolean.TRUE.equals(req.active()) ? 1 : 0);
        if (req.sortOrder() != null) c.setSortOrder(req.sortOrder());
        c.getTranslations().clear();
        applyChecklistTranslations(c, req.translations());
        c.getTasks().clear();
        applyChecklistTasks(c, req.tasks(), false);
        Checklist saved = checklists.save(c);
        audit("CHECKLIST_UPDATE", saved.getId(), actorId, null);
        return V22Dtos.ChecklistView.from(saved, saved.getTranslations(), saved.getTasks());
    }

    @Transactional
    public void deleteChecklist(Long id, Long actorId) {
        Checklist c = checklists.findById(id)
                .orElseThrow(() -> AppException.notFound("Checklist not found"));
        c.setIsActive(0);
        checklists.save(c);
        audit("CHECKLIST_DISABLE", id, actorId, null);
    }

    /** Staff marks a task complete/skipped. */
    @Transactional
    public V22Dtos.ChecklistCompletionView completeTask(Long userId,
                                                        V22Dtos.ChecklistCompleteRequest req) {
        ChecklistTask t = checklistTasks.findById(req.taskId())
                .orElseThrow(() -> AppException.notFound("Checklist task not found"));
        if (t.getIsActive() == null || t.getIsActive() == 0) {
            throw AppException.conflict("TASK_DISABLED", "Task is disabled");
        }
        if ("SKIPPED".equals(req.status())
                && (t.getIsRequired() == null || t.getIsRequired() == 1)) {
            throw AppException.conflict("CANNOT_SKIP", "Required tasks cannot be skipped");
        }
        ChecklistCompletion c = new ChecklistCompletion();
        c.setTaskId(req.taskId());
        c.setChecklistId(t.getChecklist().getId());
        c.setUserId(userId);
        c.setShiftId(req.shiftId());
        c.setStatus(parseCompletion(req.status()));
        c.setNotes(req.notes());
        c.setPhotoUrl(req.photoUrl());
        ChecklistCompletion saved = checklistCompletions.save(c);
        User u = users.findById(userId).orElse(null);
        activity("CHECKLIST_COMPLETE", "checklist_completion", saved.getId().toString(),
                userId, null,
                payloadOf("taskId", req.taskId(), "status", req.status()));
        audit("CHECKLIST_COMPLETE", saved.getId(), userId, null);
        return V22Dtos.ChecklistCompletionView.from(saved, u == null ? null : fullName(u));
    }

    @Transactional(readOnly = true)
    public List<V22Dtos.ChecklistCompletionView> recentCompletions(Long userId, int limit) {
        User u = users.findById(userId).orElse(null);
        String userName = u == null ? null : fullName(u);
        return checklistCompletions.findAllByUserIdOrderByCompletedAtDesc(userId).stream()
                .limit(Math.max(1, limit))
                .map(c -> V22Dtos.ChecklistCompletionView.from(c, userName))
                .toList();
    }

    // ====================== Check-ins ======================

    @Transactional
    public V22Dtos.CheckInView checkIn(Long userId, V22Dtos.CheckInRequest req, String ip) {
        Zone z = zones.findById(req.zoneId())
                .orElseThrow(() -> AppException.notFound("Zone not found"));
        if (z.getStatus() == Zone.Status.DISABLED) {
            throw AppException.conflict("ZONE_DISABLED", "Zone is disabled");
        }
        CheckInLog.Action action = parseAction(req.action());
        // Toggle business rule: cannot CHECK_OUT without a CHECK_IN.
        Optional<CheckInLog> last = checkInLogs
                .findFirstByUserIdAndActionOrderByCreatedAtDesc(userId, CheckInLog.Action.CHECK_IN);
        if (action == CheckInLog.Action.CHECK_OUT && last.isEmpty()) {
            throw AppException.conflict("NO_CHECK_IN", "Cannot check out without a prior check-in");
        }
        // Don't allow two open CHECK_INs in a row.
        if (action == CheckInLog.Action.CHECK_IN) {
            Optional<CheckInLog> latest = checkInLogs
                    .findFirstByUserIdAndActionOrderByCreatedAtDesc(userId, CheckInLog.Action.CHECK_IN);
            // Quick sanity: if there is no CHECK_OUT after the latest CHECK_IN, reject.
            // Implementation: re-query all events and find the most recent.
            if (hasOpenCheckIn(userId)) {
                throw AppException.conflict("ALREADY_CHECKED_IN", "Already checked in");
            }
            // Suppress the unused warning for `last` in this branch only.
            if (latest.isEmpty()) { /* no prior check-in */ }
        }
        CheckInLog log = new CheckInLog();
        log.setUserId(userId);
        log.setZoneId(req.zoneId());
        log.setAction(action);
        log.setNotes(req.notes());
        log.setDeviceId(req.deviceId());
        log.setClientIp(ip);
        CheckInLog saved = checkInLogs.save(log);
        User u = users.findById(userId).orElse(null);
        activity(action == CheckInLog.Action.CHECK_IN ? "CHECK_IN" : "CHECK_OUT",
                "check_in_log", saved.getId().toString(),
                userId, null,
                payloadOf("zoneId", req.zoneId()));
        return V22Dtos.CheckInView.from(saved, u == null ? null : fullName(u), z);
    }

    @Transactional(readOnly = true)
    public List<V22Dtos.CheckInView> recentCheckIns(Long userId, int limit) {
        return toCheckInViews(checkInLogs.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().limit(Math.max(1, limit)).toList());
    }

    /**
     * Phase G — internal helper used by the admin browse endpoint.
     * Resolves user + zone display names for the supplied rows. Caller
     * decides what to slice / filter.
     */
    @Transactional(readOnly = true)
    public List<V22Dtos.CheckInView> toCheckInViews(List<CheckInLog> rows) {
        Map<Long, String> userNameCache = new HashMap<>();
        Map<Long, Zone> zoneCache = new HashMap<>();
        return rows.stream().map(l -> {
            String userName = userNameCache.computeIfAbsent(l.getUserId(),
                    uid -> users.findById(uid).map(this::fullName).orElse(null));
            Zone z = zoneCache.computeIfAbsent(l.getZoneId(),
                    id -> zones.findById(id).orElse(null));
            return V22Dtos.CheckInView.from(l, userName, z);
        }).toList();
    }

    /** Phase G — exposes the repository for the admin controller. */
    public CheckInLogRepository checkInRepo() { return checkInLogs; }

    // ====================== Activity logs (admin) ======================

    @Transactional(readOnly = true)
    public List<V22Dtos.ActivityLogView> recentActivity(int limit,
                                                        String action,
                                                        String entity) {
        int capped = Math.max(1, Math.min(limit, 500));
        List<ActivityLog> rows;
        if (action != null && !action.isBlank()) {
            rows = activityLogs.findAllByActionOrderByCreatedAtDesc(action);
        } else if (entity != null && !entity.isBlank()) {
            rows = activityLogs.findAllByEntityAndEntityIdOrderByCreatedAtDesc(entity, null);
        } else {
            rows = activityLogs.findAll().stream()
                    .sorted((x, y) -> y.getCreatedAt().compareTo(x.getCreatedAt()))
                    .toList();
        }
        rows = rows.stream().limit(capped).toList();
        Map<Long, String> userNames = new HashMap<>();
        return rows.stream().map(r -> {
            String actorName = r.getActorUserId() == null ? null
                    : userNames.computeIfAbsent(r.getActorUserId(),
                            id -> users.findById(id).map(this::fullName).orElse(null));
            String targetName = r.getTargetUserId() == null ? null
                    : userNames.computeIfAbsent(r.getTargetUserId(),
                            id -> users.findById(id).map(this::fullName).orElse(null));
            return V22Dtos.ActivityLogView.from(r, actorName, targetName);
        }).toList();
    }

    // ---------------- helpers ----------------

    private boolean hasOpenCheckIn(Long userId) {
        // The latest event for the user — if it's CHECK_IN, the user is still inside.
        List<CheckInLog> recent = checkInLogs.findAllByUserIdOrderByCreatedAtDesc(userId);
        if (recent.isEmpty()) return false;
        CheckInLog latest = recent.get(0);
        return latest.getAction() == CheckInLog.Action.CHECK_IN;
    }

    private void applyShiftFields(Shift s, V22Dtos.ShiftRequest req) {
        s.setName(req.name());
        s.setDescription(req.description());
        s.setStartTime(req.startTime());
        s.setEndTime(req.endTime());
        if (req.tz() != null && !req.tz().isBlank()) s.setTz(req.tz());
        s.setIsActive(Boolean.TRUE.equals(req.active()) ? 1 : 0);
        if (req.sortOrder() != null) s.setSortOrder(req.sortOrder());
    }

    private void applyZoneFields(Zone z, V22Dtos.ZoneRequest req) {
        z.setCode(req.code());
        z.setColor(req.color());
        z.setStatus(Zone.Status.valueOf(req.status()));
        z.setSortOrder(req.sortOrder());
        z.setRequiredStaff(req.requiredStaff());
        z.getTranslations().clear();
        if (req.translations() != null) {
            for (V22Dtos.ZoneTranslationInput in : req.translations()) {
                ZoneTranslation t = new ZoneTranslation();
                t.setZone(z);
                t.setLanguageCode(in.lang());
                t.setName(in.name());
                t.setDescription(in.description());
                z.getTranslations().add(t);
            }
        }
    }

    private void applyChecklistTranslations(Checklist c, List<V22Dtos.ChecklistTranslationInput> inputs) {
        if (inputs == null) return;
        for (V22Dtos.ChecklistTranslationInput in : inputs) {
            ChecklistTranslation t = new ChecklistTranslation();
            t.setChecklist(c);
            t.setLanguageCode(in.lang());
            t.setTitle(in.title());
            t.setDescription(in.description());
            c.getTranslations().add(t);
        }
    }

    private void applyChecklistTasks(Checklist c,
                                    List<V22Dtos.ChecklistTaskInput> inputs,
                                    boolean newEntities) {
        if (inputs == null) return;
        for (V22Dtos.ChecklistTaskInput in : inputs) {
            ChecklistTask t;
            if (newEntities || in.id() == null) {
                t = new ChecklistTask();
                t.setChecklist(c);
            } else {
                t = c.getTasks().stream().filter(x -> x.getId().equals(in.id())).findFirst().orElse(null);
                if (t == null) {
                    t = new ChecklistTask();
                    t.setChecklist(c);
                }
            }
            t.setIsRequired(Boolean.TRUE.equals(in.required()) ? 1 : 0);
            t.setIsActive(Boolean.TRUE.equals(in.active()) ? 1 : 0);
            t.setSortOrder(in.sortOrder() == null ? 0 : in.sortOrder());
            for (V22Dtos.ChecklistTaskTranslationInput tin : in.translations() == null
                    ? List.<V22Dtos.ChecklistTaskTranslationInput>of() : in.translations()) {
                ChecklistTaskTranslation tt = new ChecklistTaskTranslation();
                tt.setTask(t);
                tt.setLanguageCode(tin.lang());
                tt.setTitle(tin.title());
                tt.setDescription(tin.description());
                t.getTranslations().add(tt);
            }
            c.getTasks().add(t);
        }
    }

    private ShiftAssignment.Status parseStatus(String s) {
        try {
            return ShiftAssignment.Status.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw AppException.badRequest("INVALID_STATUS", "Unknown shift status: " + s);
        }
    }

    private ChecklistCompletion.Status parseCompletion(String s) {
        try {
            return ChecklistCompletion.Status.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw AppException.badRequest("INVALID_STATUS", "Unknown completion status: " + s);
        }
    }

    private CheckInLog.Action parseAction(String s) {
        try {
            return CheckInLog.Action.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw AppException.badRequest("INVALID_ACTION", "Unknown action: " + s);
        }
    }

    private String fullName(User u) {
        if (u == null) return null;
        if (u.getFullName() != null && !u.getFullName().isBlank()) return u.getFullName();
        return u.getUsername();
    }

    private void activity(String action, String entity, String entityId,
                          Long actorId, Long targetId, String metadataJson) {
        try {
            ActivityLog a = new ActivityLog();
            a.setAction(action);
            a.setEntity(entity);
            a.setEntityId(entityId);
            a.setActorUserId(actorId);
            a.setTargetUserId(targetId);
            a.setMetadataJson(metadataJson);
            a.setResult(ActivityLog.Result.SUCCESS);
            activityLogs.save(a);
        } catch (Exception ex) {
            // Never let logging crash the request path.
            log.warn("activity log failure: {}", ex.getMessage());
        }
    }

    private void audit(String action, Long entityId, Long actorId, String details) {
        try {
            AuditLog a = new AuditLog();
            a.setAction(action);
            a.setEntity("v22");
            a.setEntityId(entityId == null ? null : entityId.toString());
            a.setUserId(actorId);
            a.setDetails(details);
            auditLogs.save(a);
        } catch (Exception ignored) {}
    }

    private String payloadOf(Object... kv) {
        // Always even-numbered args.
        Map<String, Object> root = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            root.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void notifyShiftAssignment(ShiftAssignment a, User u,
                                       String type, String viBody, String koBody) {
        notifyShiftAssignment2(a.getUserId(), u, type, viBody, koBody,
                Map.of("assignmentId", a.getId().toString(),
                        "shiftId", a.getShiftId() == null ? "" : a.getShiftId().toString(),
                        "date", a.getDate() == null ? "" : a.getDate()));
    }

    private void notifyShiftAssignment2(Long userId, User u,
                                        String type, String viBody, String koBody,
                                        Map<String, String> extra) {
        if (u == null || !"ACTIVE".equals(u.getStatus().name())) return;
        Map<String, String> titles = new HashMap<>();
        titles.put("vi", viBody);
        titles.put("ko", koBody);
        Map<String, String> bodies = new HashMap<>();
        bodies.put("vi", viBody);
        bodies.put("ko", koBody);
        Map<String, String> data = new HashMap<>(extra);
        data.put("type", type);
        try {
            notifications.createAndDispatch(userId, type, titles, bodies,
                    objectMapper.writeValueAsString(data),
                    "v22:" + type + ":" + userId + ":" + data.get("assignmentId"));
        } catch (Exception ex) {
            log.warn("notifyShiftAssignment2 failed: {}", ex.getMessage());
        }
    }

    /** Send a fan-out push to every active admin/staff for situational alerts. */
    private void notifyAdmins(String type, String viBody, String koBody,
                               Map<String, String> data) {
        try {
            List<User> staff = users.findAll().stream()
                    .filter(u -> u.getRole() == User.Role.ADMIN && u.getStatus() == User.Status.ACTIVE)
                    .toList();
            for (User admin : staff) {
                notifyShiftAssignment2(admin.getId(), admin, type, viBody, koBody, data);
            }
        } catch (Exception ex) {
            log.warn("notifyAdmins failed: {}", ex.getMessage());
        }
    }
}

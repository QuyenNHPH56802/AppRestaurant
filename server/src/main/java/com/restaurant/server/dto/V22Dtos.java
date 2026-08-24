package com.restaurant.server.dto;

import com.restaurant.server.entity.CheckInLog;
import com.restaurant.server.entity.Checklist;
import com.restaurant.server.entity.ChecklistCompletion;
import com.restaurant.server.entity.ChecklistTask;
import com.restaurant.server.entity.ChecklistTaskTranslation;
import com.restaurant.server.entity.ChecklistTranslation;
import com.restaurant.server.entity.Shift;
import com.restaurant.server.entity.ShiftAssignment;
import com.restaurant.server.entity.Zone;
import com.restaurant.server.entity.ZoneAssignment;
import com.restaurant.server.entity.ZoneTranslation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * V2.2 DTOs for shifts, zones, checklists, check-ins and activity logs.
 *
 * <p>Read-only {@code *View} records (used by both admin and staff endpoints)
 * live here alongside the request bodies to keep the contract in one place.
 * The Android app consumes only the {@code Me*} prefix variants; admins get
 * the unprefixed variants which include soft-delete / audit fields.</p>
 */
public class V22Dtos {

    // -------------------- Shifts --------------------

    public record ShiftView(
            Long id,
            String name,
            String description,
            String startTime,
            String endTime,
            String tz,
            boolean active,
            int sortOrder
    ) {
        public static ShiftView from(Shift s) {
            return new ShiftView(
                    s.getId(), s.getName(), s.getDescription(),
                    s.getStartTime(), s.getEndTime(), s.getTz(),
                    s.getIsActive() != null && s.getIsActive() == 1,
                    s.getSortOrder() == null ? 0 : s.getSortOrder());
        }
    }

    public record ShiftRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description,
            @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String startTime,
            @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String endTime,
            String tz,
            Boolean active,
            Integer sortOrder
    ) {}

    public record ShiftAssignmentView(
            Long id,
            Long shiftId,
            String shiftName,
            String shiftStartTime,
            String shiftEndTime,
            Long userId,
            String userName,
            String date,
            String status,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ShiftAssignmentView from(ShiftAssignment a,
                                               Shift s,
                                               String userFullName) {
            return new ShiftAssignmentView(
                    a.getId(),
                    a.getShiftId(),
                    s == null ? null : s.getName(),
                    s == null ? null : s.getStartTime(),
                    s == null ? null : s.getEndTime(),
                    a.getUserId(),
                    userFullName,
                    a.getDate(),
                    a.getStatus() == null ? null : a.getStatus().name(),
                    a.getNotes(),
                    a.getCreatedAt(),
                    a.getUpdatedAt());
        }
    }

    /** Admin create/update body for a shift assignment. */
    public record ShiftAssignmentRequest(
            @NotNull Long shiftId,
            @NotNull Long userId,
            @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") String date,
            String status,
            @Size(max = 500) String notes
    ) {}

    /** Staff self-service: accept / reject / change / cancel an existing assignment. */
    public record ShiftAssignmentRespondRequest(
            @NotBlank @Pattern(regexp = "ACCEPTED|REJECTED|CHANGE_REQUESTED|CANCELLED") String status,
            @Size(max = 500) String notes
    ) {}

    // -------------------- Zones --------------------

    public record ZoneTranslationView(String lang, String name, String description) {
        public static ZoneTranslationView from(ZoneTranslation t) {
            return new ZoneTranslationView(t.getLanguageCode(), t.getName(), t.getDescription());
        }
    }

    public record ZoneView(
            Long id,
            String code,
            String color,
            String status,
            int sortOrder,
            int requiredStaff,
            int currentStaff,
            String staffingStatus,  // OK (>= required) | SHORT (1 thiếu) | EMPTY | UNKNOWN
            boolean currentAssignment,
            List<ZoneTranslationView> translations
    ) {
        public static ZoneView from(Zone z, List<ZoneTranslation> translations) {
            return from(z, translations, 0);
        }

        /**
         * @param z the zone entity
         * @param translations translations for the zone
         * @param currentStaff how many staff are currently assigned (is_current=1)
         */
        public static ZoneView from(Zone z, List<ZoneTranslation> translations, int currentStaff) {
            int required = z.getRequiredStaff() == null ? 0 : z.getRequiredStaff();
            String staffing;
            if (z.getStatus() != Zone.Status.ACTIVE) {
                staffing = "DISABLED";
            } else if (required == 0) {
                staffing = currentStaff > 0 ? "OK" : "UNKNOWN";
            } else if (currentStaff >= required) {
                staffing = "OK";
            } else if (currentStaff == 0) {
                staffing = "EMPTY";
            } else {
                staffing = "SHORT";
            }
            return new ZoneView(
                    z.getId(), z.getCode(), z.getColor(),
                    z.getStatus() == null ? "ACTIVE" : z.getStatus().name(),
                    z.getSortOrder() == null ? 0 : z.getSortOrder(),
                    required, currentStaff, staffing,
                    false,
                    translations.stream().map(ZoneTranslationView::from).toList());
        }
    }

    /** Lightweight DTO returned when an admin regenerates a zone QR token. */
    public record ZoneQrTokenView(Long zoneId, String qrToken) {}

    public record ZoneRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
            @NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status,
            @Min(0) @Max(999) int sortOrder,
            @Min(0) @Max(99) int requiredStaff,
            List<ZoneTranslationInput> translations
    ) {}

    public record ZoneTranslationInput(
            @NotBlank @Pattern(regexp = "vi|ko") String lang,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String description
    ) {}

    public record ZoneAssignmentView(
            Long id,
            Long userId,
            String userName,
            Long zoneId,
            String zoneCode,
            String zoneName,
            Instant effectiveFrom,
            Instant effectiveTo,
            boolean current
    ) {
        public static ZoneAssignmentView from(ZoneAssignment a,
                                               String userFullName,
                                               Zone z) {
            return new ZoneAssignmentView(
                    a.getId(), a.getUserId(), userFullName,
                    a.getZoneId(),
                    z == null ? null : z.getCode(),
                    z == null ? null : nameOf(z, "vi"),
                    a.getEffectiveFrom(), a.getEffectiveTo(),
                    a.getIsCurrent() != null && a.getIsCurrent() == 1);
        }
    }

    /** Admin / staff request to (re-)assign a user to a zone. */
    public record ZoneAssignRequest(
            @NotNull Long userId,
            @NotNull Long zoneId,
            @Size(max = 500) String reason
    ) {}

    // -------------------- Checklists --------------------

    public record ChecklistTaskView(
            Long id,
            Long checklistId,
            boolean required,
            boolean active,
            int sortOrder,
            List<ChecklistTaskTranslationView> translations
    ) {
        public static ChecklistTaskView from(ChecklistTask t,
                                             List<ChecklistTaskTranslation> trs) {
            return new ChecklistTaskView(
                    t.getId(), t.getChecklist().getId(),
                    t.getIsRequired() != null && t.getIsRequired() == 1,
                    t.getIsActive() != null && t.getIsActive() == 1,
                    t.getSortOrder() == null ? 0 : t.getSortOrder(),
                    trs.stream().map(ChecklistTaskTranslationView::from).toList());
        }
    }

    public record ChecklistTaskTranslationView(String lang, String title, String description) {
        public static ChecklistTaskTranslationView from(ChecklistTaskTranslation t) {
            return new ChecklistTaskTranslationView(
                    t.getLanguageCode(), t.getTitle(), t.getDescription());
        }
    }

    public record ChecklistView(
            Long id,
            Long zoneId,
            String zoneCode,
            String zoneName,
            boolean active,
            int sortOrder,
            List<ChecklistTranslationView> translations,
            List<ChecklistTaskView> tasks
    ) {
        public static ChecklistView from(Checklist c,
                                         List<ChecklistTranslation> trs,
                                         List<ChecklistTask> tasks) {
            Zone z = c.getZone();
            return new ChecklistView(
                    c.getId(),
                    z == null ? null : z.getId(),
                    z == null ? null : z.getCode(),
                    z == null ? null : nameOf(z, "vi"),
                    c.getIsActive() != null && c.getIsActive() == 1,
                    c.getSortOrder() == null ? 0 : c.getSortOrder(),
                    trs.stream().map(ChecklistTranslationView::from).toList(),
                    tasks.stream().map(t -> ChecklistTaskView.from(t, t.getTranslations())).toList());
        }
    }

    public record ChecklistTranslationView(String lang, String title, String description) {
        public static ChecklistTranslationView from(ChecklistTranslation t) {
            return new ChecklistTranslationView(
                    t.getLanguageCode(), t.getTitle(), t.getDescription());
        }
    }

    public record ChecklistRequest(
            @NotNull Long zoneId,
            Boolean active,
            Integer sortOrder,
            List<ChecklistTranslationInput> translations,
            List<ChecklistTaskInput> tasks
    ) {}

    public record ChecklistTranslationInput(
            @NotBlank @Pattern(regexp = "vi|ko") String lang,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 500) String description
    ) {}

    public record ChecklistTaskInput(
            Long id,
            Boolean required,
            Boolean active,
            Integer sortOrder,
            List<ChecklistTaskTranslationInput> translations
    ) {}

    public record ChecklistTaskTranslationInput(
            @NotBlank @Pattern(regexp = "vi|ko") String lang,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 500) String description
    ) {}

    /** Body for staff marking a task completed/skipped. */
    public record ChecklistCompleteRequest(
            @NotNull Long taskId,
            @NotBlank @Pattern(regexp = "COMPLETED|SKIPPED") String status,
            @Size(max = 500) String notes,
            @Size(max = 1024) String photoUrl,
            Long shiftId
    ) {}

    public record ChecklistCompletionView(
            Long id,
            Long taskId,
            Long checklistId,
            Long userId,
            String userName,
            String status,
            String notes,
            String photoUrl,
            Long shiftId,
            Instant completedAt
    ) {
        public static ChecklistCompletionView from(ChecklistCompletion c, String userFullName) {
            return new ChecklistCompletionView(
                    c.getId(), c.getTaskId(), c.getChecklistId(),
                    c.getUserId(), userFullName,
                    c.getStatus().name(),
                    c.getNotes(), c.getPhotoUrl(),
                    c.getShiftId(), c.getCompletedAt());
        }
    }

    // -------------------- Check-ins --------------------

    public record CheckInRequest(
            @NotNull Long zoneId,
            @NotBlank @Pattern(regexp = "CHECK_IN|CHECK_OUT") String action,
            @Size(max = 500) String notes,
            @Size(max = 256) String deviceId
    ) {}

    public record CheckInView(
            Long id,
            Long userId,
            String userName,
            Long zoneId,
            String zoneCode,
            String action,
            String notes,
            String deviceId,
            Instant createdAt
    ) {
        public static CheckInView from(CheckInLog log, String userFullName, Zone zone) {
            return new CheckInView(
                    log.getId(), log.getUserId(), userFullName, log.getZoneId(),
                    zone == null ? null : zone.getCode(),
                    log.getAction().name(),
                    log.getNotes(), log.getDeviceId(), log.getCreatedAt());
        }
    }

    // -------------------- Activity logs (admin) --------------------

    public record ActivityLogView(
            Long id,
            Long actorUserId,
            String actorName,
            String action,
            String entity,
            String entityId,
            Long targetUserId,
            String targetName,
            String metadataJson,
            String ip,
            String userAgent,
            String result,
            Instant createdAt
    ) {
        public static ActivityLogView from(com.restaurant.server.entity.ActivityLog log,
                                           String actorName,
                                           String targetName) {
            return new ActivityLogView(
                    log.getId(),
                    log.getActorUserId(), actorName,
                    log.getAction(), log.getEntity(), log.getEntityId(),
                    log.getTargetUserId(), targetName,
                    log.getMetadataJson(), log.getIp(), log.getUserAgent(),
                    log.getResult() == null ? "SUCCESS" : log.getResult().name(),
                    log.getCreatedAt());
        }
    }

    private static String nameOf(Zone z, String lang) {
        if (z == null || z.getTranslations() == null) return null;
        for (ZoneTranslation t : z.getTranslations()) {
            if (lang.equals(t.getLanguageCode())) return t.getName();
        }
        return z.getTranslations().isEmpty() ? null : z.getTranslations().get(0).getName();
    }
}

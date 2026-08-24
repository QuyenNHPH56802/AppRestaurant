package com.restaurant.server.controller;

import com.restaurant.server.backup.BackupService;
import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/backup")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBackupController {

    private final BackupService service;
    private final MessageService messages;

    public AdminBackupController(BackupService service, MessageService messages) {
        this.service = service;
        this.messages = messages;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BackupService.BackupFile>> run() {
        try {
            var f = service.runBackup();
            return ResponseEntity.ok(ApiResponse.ok(
                new BackupService.BackupFile(f.getFileName().toString(), f.toFile().length(),
                        java.time.Instant.ofEpochMilli(f.toFile().lastModified()))));
        } catch (Exception e) {
            throw AppException.badRequest("BACKUP_FAILED", messages.get("error.internal") + ": " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<BackupListResponse>> list() {
        List<BackupService.BackupFile> files = service.list();
        return ResponseEntity.ok(ApiResponse.ok(new BackupListResponse(files)));
    }

    public record BackupListResponse(List<BackupService.BackupFile> files) {}
}
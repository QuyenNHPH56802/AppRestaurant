package com.restaurant.server.controller;

import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.network.LanIpDetector;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;

/**
 * Server-level admin info: uptime, data dir size, version, JVM info.
 */
@RestController
@RequestMapping("/api/admin/server")
@PreAuthorize("hasRole('ADMIN')")
public class AdminServerController {

    private static final Instant STARTED_AT = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime());

    private final LanIpDetector ip;

    public AdminServerController(LanIpDetector ip) {
        this.ip = ip;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> status() {
        long uptimeMs = Duration.between(STARTED_AT, Instant.now()).toMillis();
        File dataDir = new File(System.getProperty("user.home"), "RestaurantServer/data");
        long dataSize = dirSize(dataDir);
        File backupsDir = new File(System.getProperty("user.home"), "RestaurantServer/backups");
        int backupCount = backupsDir.isDirectory() ? backupsDir.listFiles() == null ? 0 : backupsDir.listFiles().length : 0;
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.ofEntries(
            java.util.Map.entry("server", ip.getLanIp()),
            java.util.Map.entry("port", ip.getPort()),
            java.util.Map.entry("protocol", ip.getProtocol()),
            java.util.Map.entry("version", "1.0.0"),
            java.util.Map.entry("startedAt", STARTED_AT.toString()),
            java.util.Map.entry("uptimeMs", uptimeMs),
            java.util.Map.entry("dataDir", dataDir.getAbsolutePath()),
            java.util.Map.entry("dataSizeBytes", dataSize),
            java.util.Map.entry("backupCount", backupCount),
            java.util.Map.entry("java", System.getProperty("java.version")),
            java.util.Map.entry("os", System.getProperty("os.name") + " " + System.getProperty("os.version"))
        )));
    }

    private long dirSize(File dir) {
        if (!dir.isDirectory()) return 0L;
        long total = 0L;
        File[] children = dir.listFiles();
        if (children == null) return 0L;
        for (File f : children) {
            if (f.isFile()) total += f.length();
            else if (f.isDirectory()) total += dirSize(f);
        }
        return total;
    }
}
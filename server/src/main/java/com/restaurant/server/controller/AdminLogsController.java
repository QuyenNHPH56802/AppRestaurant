package com.restaurant.server.controller;

import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tail the server's log file. Read-only; the admin SPA polls this endpoint.
 */
@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogsController {

    private final RestaurantProperties props;

    public AdminLogsController(RestaurantProperties props) {
        this.props = props;
    }

    @GetMapping("/tail")
    public ResponseEntity<ApiResponse<LogTailResponse>> tail(
            @RequestParam(defaultValue = "200") int lines,
            @RequestParam(defaultValue = "server.log") String file) {
        int safe = Math.max(10, Math.min(lines, 2000));
        Path target = Paths.get(props.getLogsDir()).resolve(sanitize(file));
        if (!Files.isRegularFile(target)) {
            return ResponseEntity.ok(ApiResponse.ok(new LogTailResponse(file, List.of())));
        }
        try {
            List<String> all = Files.readAllLines(target);
            int from = Math.max(0, all.size() - safe);
            List<String> tail = new ArrayList<>(all.subList(from, all.size()));
            Collections.reverse(tail);
            return ResponseEntity.ok(ApiResponse.ok(new LogTailResponse(file, tail)));
        } catch (IOException e) {
            return ResponseEntity.ok(ApiResponse.ok(new LogTailResponse(file, List.of("(read failed: " + e.getMessage() + ")"))));
        }
    }

    private String sanitize(String s) {
        if (s == null || s.isBlank()) return "server.log";
        // Path traversal guard
        if (s.contains("..") || s.contains("/") || s.contains("\\")) return "server.log";
        return s;
    }

    public record LogTailResponse(String file, List<String> lines) {}
}
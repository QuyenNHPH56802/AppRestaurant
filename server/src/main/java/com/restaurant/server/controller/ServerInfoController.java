package com.restaurant.server.controller;

import com.restaurant.server.dto.ApiResponse;
import com.restaurant.server.dto.ServerInfoResponse;
import com.restaurant.server.network.LanIpDetector;
import com.restaurant.server.network.QrCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ServerInfoController {

    private final LanIpDetector ip;
    private final QrCodeService qr;
    private final String version;

    public ServerInfoController(LanIpDetector ip, QrCodeService qr,
                                @Value("${spring.application.name:restaurant-server}") String appName) {
        this.ip = ip;
        this.qr = qr;
        this.version = appName + " 1.0.0";
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of(
            "status", "ok",
            "version", version,
            "timestamp", java.time.Instant.now().toString()
        )));
    }

    @GetMapping("/server/info")
    public ResponseEntity<ApiResponse<ServerInfoResponse>> serverInfo() {
        return ResponseEntity.ok(ApiResponse.ok(
                new ServerInfoResponse(ip.getLanIp(), ip.getPort(), ip.getProtocol(), version)
        ));
    }

    @GetMapping("/server/qr.png")
    public ResponseEntity<byte[]> qrPng(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "360") int size) {
        int safe = Math.max(120, Math.min(size, 1024));
        byte[] png = qr.renderPng(safe);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .body(png);
    }
}
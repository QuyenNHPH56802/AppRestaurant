package com.restaurant.server.controller;

import com.restaurant.server.entity.Zone;
import com.restaurant.server.entity.ZoneTranslation;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.network.QrCodeService;
import com.restaurant.server.repository.ZoneRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public QR endpoints for zones. Used to generate printable QRs for each
 * workstation in the restaurant (so staff can scan and check in).
 *
 * <p>No authentication is required to render the QR image, but the encoded
 * payload includes a rotating secret token that the server validates at
 * check-in time. Treat the printed QRs as semi-secret.</p>
 */
@RestController
@RequestMapping("/api/zones")
public class ZoneQrController {

    private final ZoneRepository zones;
    private final QrCodeService qr;

    public ZoneQrController(ZoneRepository zones, QrCodeService qr) {
        this.zones = zones;
        this.qr = qr;
    }

    /**
     * Render a printable QR PNG for a zone. Resolves the display name from
     * the zone_translations table (vi by default).
     */
    @GetMapping("/{code}/qr.png")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> qrPng(
            @PathVariable String code,
            @RequestParam(defaultValue = "512") int size) {
        int safe = Math.max(160, Math.min(size, 2048));
        Zone z = zones.findByCodeWithTranslations(code)
                .orElseThrow(() -> AppException.notFound("Zone not found: " + code));
        if (z.getStatus() == Zone.Status.DISABLED) {
            throw AppException.badRequest("ZONE_DISABLED", "Zone is disabled");
        }
        if (z.getQrToken() == null || z.getQrToken().isBlank()) {
            throw AppException.badRequest("NO_QR_TOKEN", "Zone has no QR token; regenerate it first");
        }
        String name = pickDisplayName(z.getTranslations(), "vi");
        String payload = qr.zonePayloadJson(z.getCode(), z.getQrToken(), name);
        byte[] png = qr.renderPayloadPng(payload, safe);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .body(png);
    }

    private static String pickDisplayName(List<ZoneTranslation> translations, String lang) {
        if (translations == null || translations.isEmpty()) return null;
        for (ZoneTranslation t : translations) {
            if (lang.equals(t.getLanguageCode())) return t.getName();
        }
        return translations.get(0).getName();
    }
}
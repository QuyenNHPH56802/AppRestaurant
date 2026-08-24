package com.restaurant.server.network;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.restaurant.server.config.RestaurantProperties;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Generates a QR PNG that encodes the server-info JSON payload.
 * The same payload is parsed by the Android app's QrPayloadParser.
 */
@Service
public class QrCodeService {

    private final LanIpDetector ip;
    private final RestaurantProperties props;

    public QrCodeService(LanIpDetector ip, RestaurantProperties props) {
        this.ip = ip;
        this.props = props;
    }

    public String payloadJson() {
        return String.format(
            "{\"server\":\"%s\",\"port\":%d,\"protocol\":\"%s\",\"version\":\"restaurant-server 1.0.0\"}",
            ip.getLanIp(), ip.getPort(), ip.getProtocol()
        );
    }

    public byte[] renderPng(int size) {
        return renderPayloadPng(payloadJson(), size);
    }

    /**
     * Render a QR PNG containing an arbitrary string payload.
     * Used by zone QR endpoint and any future QR-bearing resource.
     */
    public byte[] renderPayloadPng(String payload, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                payload,
                BarcodeFormat.QR_CODE,
                size, size,
                Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                       EncodeHintType.MARGIN, 1)
            );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to render QR", e);
        }
    }

    /** Build the JSON payload encoded in a zone QR. */
    public String zonePayloadJson(String code, String qrToken, String displayName) {
        // Display name is best-effort; falls back to code if no translation is known.
        String safeName = displayName == null || displayName.isBlank() ? code : displayName;
        return String.format(
            "{\"type\":\"zone\",\"code\":\"%s\",\"token\":\"%s\",\"name\":\"%s\",\"server\":\"%s\",\"port\":%d}",
            code, qrToken, safeName.replace("\"", "'"),
            ip.getLanIp(), ip.getPort()
        );
    }
}
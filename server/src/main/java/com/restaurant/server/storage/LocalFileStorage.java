package com.restaurant.server.storage;

import com.restaurant.server.config.RestaurantProperties;
import com.restaurant.server.exception.AppException;
import com.restaurant.server.i18n.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Local file storage for food images. Files are written under <uploads-dir>/foods/
 * and served as static assets at /uploads/**.
 *
 * Validation: content type must be in the allow-list, size <= 5 MB, and the
 * stored filename is a UUID plus the validated extension to prevent path
 * traversal or double-extension attacks.
 */
@Service
public class LocalFileStorage {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final RestaurantProperties props;
    private final MessageService messages;

    public LocalFileStorage(RestaurantProperties props, MessageService messages) {
        this.props = props;
        this.messages = messages;
    }

    public StoredFile saveFoodImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AppException.badRequest("FILE_EMPTY", "No file uploaded");
        }
        if (file.getSize() > MAX_BYTES) {
            throw AppException.badRequest("FILE_TOO_LARGE", "File exceeds 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null) contentType = "application/octet-stream";
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(ct)) {
            throw AppException.badRequest("FILE_TYPE", "Unsupported content type: " + contentType);
        }
        String ext = extensionFromName(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            throw AppException.badRequest("FILE_EXT", "Unsupported file extension");
        }
        Path dir = Paths.get(props.getUploadsDir(), "foods");
        try {
            Files.createDirectories(dir);
            String safeName = UUID.randomUUID() + "." + ext.toLowerCase(Locale.ROOT);
            Path target = dir.resolve(safeName).normalize();
            if (!target.startsWith(dir)) {
                throw AppException.badRequest("FILE_PATH", "Invalid path");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile("/uploads/foods/" + safeName, file.getSize(), ct);
        } catch (IOException e) {
            throw AppException.badRequest("FILE_WRITE", "Failed to write file: " + e.getMessage());
        }
    }

    private String extensionFromName(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return null;
        return name.substring(i + 1);
    }

    public record StoredFile(String imageUrl, long size, String contentType) {}
}
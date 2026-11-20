package com.meocare.meocare.service;

import com.meocare.meocare.dto.FileUploadResponse;
import com.meocare.meocare.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Map<String, Set<String>> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", Set.of(".jpg", ".jpeg"),
            "image/jpg", Set.of(".jpg", ".jpeg"),
            "image/png", Set.of(".png"),
            "image/webp", Set.of(".webp"),
            "image/gif", Set.of(".gif")
    );

    private final Path uploadRoot;
    private final long maxFileSizeBytes;

    public FileStorageService(
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            @Value("${app.upload.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        String configured = (uploadDir == null || uploadDir.isBlank()) ? "./uploads" : uploadDir.trim();
        this.uploadRoot = Paths.get(configured).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public FileUploadResponse storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file must not be empty");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("Image file exceeds maximum allowed size of " + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        }

        String contentType = file.getContentType() != null
                ? file.getContentType().trim().toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Unsupported image MIME type. Allowed types: JPEG, PNG, WebP, GIF");
        }

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        Set<String> validExts = CONTENT_TYPE_EXTENSIONS.getOrDefault(contentType, Set.of());
        if (extension.isEmpty() || !validExts.contains(extension)) {
            throw new BadRequestException("File extension does not match allowed image format");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read uploaded image file", ex);
        }

        if (!matchesImageMagicBytes(bytes, contentType)) {
            throw new BadRequestException("Uploaded file content does not match a valid image signature");
        }

        String storedFileName = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadRoot);
            Path targetPath = uploadRoot.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(uploadRoot)) {
                throw new BadRequestException("Invalid target file path");
            }
            Files.write(targetPath, bytes);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded image file", ex);
        }

        String normalizedContentType = "image/jpg".equals(contentType) ? "image/jpeg" : contentType;
        return new FileUploadResponse(
                "/uploads/" + storedFileName,
                storedFileName,
                normalizedContentType,
                bytes.length
        );
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        String cleanName = Paths.get(filename).getFileName().toString();
        int dotIndex = cleanName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleanName.length() - 1) {
            return "";
        }
        return cleanName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private boolean matchesImageMagicBytes(byte[] data, String contentType) {
        if (data == null || data.length < 4) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg", "image/jpg" ->
                    (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
            case "image/png" -> data.length >= 8
                    && (data[0] & 0xFF) == 0x89
                    && data[1] == 'P' && data[2] == 'N' && data[3] == 'G'
                    && (data[4] & 0xFF) == 0x0D && (data[5] & 0xFF) == 0x0A
                    && (data[6] & 0xFF) == 0x1A && (data[7] & 0xFF) == 0x0A;
            case "image/gif" -> data.length >= 6
                    && data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8'
                    && (data[4] == '7' || data[4] == '9') && data[5] == 'a';
            case "image/webp" -> data.length >= 12
                    && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                    && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
            default -> false;
        };
    }
}

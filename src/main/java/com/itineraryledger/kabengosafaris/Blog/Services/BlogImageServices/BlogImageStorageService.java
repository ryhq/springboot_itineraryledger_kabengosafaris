package com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.FileSettings.FileSettingGetterServices;
import com.itineraryledger.kabengosafaris.ImageSettings.ImageSettingGetterServices;

import lombok.extern.slf4j.Slf4j;

/**
 * Where a blog image lives on disk, and whether it is allowed in.
 *
 * The same shape as the accommodation and testimony image storage services: the size and type
 * rules come from the configured file/image settings rather than being hard-coded here, so
 * changing them in Settings changes them everywhere.
 */
@Service
@Slf4j
public class BlogImageStorageService {

    @Value("${blog.image.storage.path:./data/blog-images/}")
    private String storagePath;

    @Value("${app.base.url:http://localhost:4450}")
    private String appBaseUrl;

    @Autowired
    private ImageSettingGetterServices imageSettingGetterServices;

    @Autowired
    private FileSettingGetterServices fileSettingGetterServices;

    public void initializeStorageDirectory() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created blog image storage directory: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("Failed to create blog image storage directory: {}", storagePath, e);
            throw new RuntimeException("Failed to initialize blog image storage directory", e);
        }
    }

    public String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return "No file provided";
        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();
        Long maxFileSize = fileSettingGetterServices.getMaxFileSize();
        if (fileSize > maxFileSize) {
            return String.format("File size (%s) exceeds maximum allowed size (%s)",
                formatFileSize(fileSize), fileSettingGetterServices.getMaxFileSizeString());
        }
        return imageSettingGetterServices.validateImageUpload(originalFilename, fileSize);
    }

    public String validateRequestSize(long totalSize) {
        Long maxRequestSize = fileSettingGetterServices.getMaxRequestSize();
        if (totalSize > maxRequestSize) {
            return String.format("Total request size (%s) exceeds maximum allowed size (%s)",
                formatFileSize(totalSize), fileSettingGetterServices.getMaxRequestSizeString());
        }
        return null;
    }

    public String saveImage(MultipartFile file) {
        try {
            initializeStorageDirectory();
            String validationError = validateImage(file);
            if (validationError != null) {
                log.warn("Blog image validation failed: {}", validationError);
                return null;
            }
            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String generatedFilename = generateHashedFilename(originalFilename, extension);
            Path targetPath = Paths.get(storagePath, generatedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Blog image saved: {} (original: {})", generatedFilename, originalFilename);
            return generatedFilename;
        } catch (IOException e) {
            log.error("Failed to save blog image: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    public boolean deleteImage(String fileName) {
        try {
            Path filePath = Paths.get(storagePath, fileName);
            if (!Files.exists(filePath)) {
                log.warn("Blog image file not found for deletion: {}", fileName);
                return false;
            }
            Files.delete(filePath);
            log.info("Blog image deleted: {}", fileName);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete blog image: {}", fileName, e);
            return false;
        }
    }

    public boolean imageExists(String fileName) {
        return Files.exists(Paths.get(storagePath, fileName));
    }

    public byte[] readImage(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(storagePath, fileName));
    }

    public Path resolve(String fileName) {
        return Paths.get(storagePath, fileName);
    }

    /** The URL the panel and the website both use: the API serves the bytes. */
    public String constructImageUrl(String obfuscatedId) {
        if (obfuscatedId == null || obfuscatedId.isBlank()) return null;
        return appBaseUrl + "/api/blog-images/" + obfuscatedId + "/file";
    }

    public String constructFileUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        return appBaseUrl + "/api/blog-images/file/" + fileName;
    }

    public String formatFileSize(Long bytes) {
        if (bytes == null) return null;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public String detectMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".avif")) return "image/avif";
        return "image/jpeg";
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * A content-addressed name.
     *
     * Two uploads of the same picture land on the same file rather than filling the disk with
     * copies, and nothing user-supplied ends up in a path.
     */
    private String generateHashedFilename(String originalFilename, String extension) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String seed = (originalFilename == null ? "blog" : originalFilename) + System.nanoTime();
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 16; i++) hex.append(String.format("%02x", hash[i]));
            return hex + extension;
        } catch (NoSuchAlgorithmException e) {
            return System.nanoTime() + extension;
        }
    }
}

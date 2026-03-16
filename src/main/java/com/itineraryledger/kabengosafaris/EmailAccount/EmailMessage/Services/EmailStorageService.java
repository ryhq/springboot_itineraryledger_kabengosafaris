package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailStorageService {

    private final EmailSettingGetterServices emailSettingGetterServices;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Get the base storage path from settings
     */
    private Path getBasePath() {
        return Paths.get(emailSettingGetterServices.getEmailStorageBasePath());
    }

    /**
     * Initialize directory structure for an email account
     */
    public void initializeAccountDirectories(Long accountId) {
        try {
            Path accountPath = getBasePath().resolve(String.valueOf(accountId));
            Files.createDirectories(accountPath.resolve("inbox"));
            Files.createDirectories(accountPath.resolve("sent"));
            Files.createDirectories(accountPath.resolve("drafts"));
            Files.createDirectories(accountPath.resolve("trash"));
            Files.createDirectories(accountPath.resolve("archive"));
            Files.createDirectories(accountPath.resolve("attachments"));
            log.info("Initialized email directories for account {}", accountId);
        } catch (IOException e) {
            log.error("Failed to initialize email directories for account {}", accountId, e);
        }
    }

    /**
     * Generate a unique .eml filename from the Message-ID
     */
    public String generateEmlFileName(String messageId) {
        String hash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((messageId != null ? messageId : String.valueOf(System.nanoTime())).getBytes());
            hash = HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception e) {
            hash = String.valueOf(System.nanoTime());
        }
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        return hash + "_" + timestamp + ".eml";
    }

    /**
     * Save a MimeMessage as an .eml file to disk
     */
    public boolean saveEmlFromMimeMessage(Long accountId, String folderName, String fileName, MimeMessage message) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeTo(baos);
            return saveEmlFile(accountId, folderName, fileName, baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to save MimeMessage as .eml for account {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Save raw .eml bytes to disk
     */
    public boolean saveEmlFile(Long accountId, String folderName, String fileName, byte[] emlContent) {
        try {
            Path dirPath = getBasePath().resolve(String.valueOf(accountId)).resolve(folderName.toLowerCase());
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(fileName);
            Files.write(filePath, emlContent);
            log.debug("Saved .eml file: {}", filePath);
            return true;
        } catch (IOException e) {
            log.error("Failed to save .eml file {}/{}/{}: {}", accountId, folderName, fileName, e.getMessage());
            return false;
        }
    }

    /**
     * Read .eml file from disk
     */
    public byte[] readEmlFile(Long accountId, String storagePath, String fileName) {
        try {
            Path filePath = getBasePath().resolve(String.valueOf(accountId)).resolve(storagePath).resolve(fileName);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
            log.warn("EML file not found: {}", filePath);
            return null;
        } catch (IOException e) {
            log.error("Failed to read .eml file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Delete .eml file from disk
     */
    public boolean deleteEmlFile(Long accountId, String storagePath, String fileName) {
        try {
            Path filePath = getBasePath().resolve(String.valueOf(accountId)).resolve(storagePath).resolve(fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete .eml file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Move .eml file between folders
     */
    public boolean moveEmlFile(Long accountId, String fromFolder, String toFolder, String fileName) {
        try {
            Path fromPath = getBasePath().resolve(String.valueOf(accountId)).resolve(fromFolder).resolve(fileName);
            Path toDir = getBasePath().resolve(String.valueOf(accountId)).resolve(toFolder);
            Files.createDirectories(toDir);
            Path toPath = toDir.resolve(fileName);
            Files.move(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Moved .eml file from {} to {}", fromPath, toPath);
            return true;
        } catch (IOException e) {
            log.error("Failed to move .eml file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Save an attachment file to disk
     */
    public boolean saveAttachment(Long accountId, String fileName, byte[] content) {
        try {
            Path dirPath = getBasePath().resolve(String.valueOf(accountId)).resolve("attachments");
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(fileName);
            Files.write(filePath, content);
            return true;
        } catch (IOException e) {
            log.error("Failed to save attachment: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Read an attachment file from disk
     */
    public byte[] readAttachment(Long accountId, String fileName) {
        try {
            Path filePath = getBasePath().resolve(String.valueOf(accountId)).resolve("attachments").resolve(fileName);
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
            return null;
        } catch (IOException e) {
            log.error("Failed to read attachment: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Delete an attachment file from disk
     */
    public boolean deleteAttachment(Long accountId, String fileName) {
        try {
            Path filePath = getBasePath().resolve(String.valueOf(accountId)).resolve("attachments").resolve(fileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete attachment: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get total storage size for an account in bytes
     */
    public long getStorageSizeForAccount(Long accountId) {
        try {
            Path accountPath = getBasePath().resolve(String.valueOf(accountId));
            if (!Files.exists(accountPath)) return 0;
            try (Stream<Path> walk = Files.walk(accountPath)) {
                return walk.filter(Files::isRegularFile).mapToLong(p -> {
                    try { return Files.size(p); } catch (IOException e) { return 0; }
                }).sum();
            }
        } catch (IOException e) {
            log.error("Failed to calculate storage size for account {}: {}", accountId, e.getMessage());
            return 0;
        }
    }
}

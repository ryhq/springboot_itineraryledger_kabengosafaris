package com.itineraryledger.kabengosafaris.CompanyProfile.Services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.CompanyProfile.DTOs.CompanyAssetDTO;
import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyAsset;
import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyProfile;
import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

/**
 * The five brand files: two logos, two favicons, one raster logo for email.
 *
 * Deliberately NOT routed through the shared image pipeline. That pipeline exists for photographs —
 * it validates against the configured image formats, which exclude SVG, and SVG is the whole point
 * here: a logo has to stay sharp on a letterhead. So this service does its own, tighter validation.
 *
 * An SVG is a document, not a picture: it can carry script, and it would be served from the API's
 * own origin. Uploads are therefore screened for active content and every response carries a
 * locked-down CSP, so a logo can never become a way to run code on the API domain.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyAssetService {

    private final CompanyProfileRepository profileRepository;
    private final CompanyProfileGetService getService;
    private final CompanyProfileWriteService writeService;
    private final CompanyIdentityService identityService;

    @Value("${company.asset.storage.path:./data/company-assets/}")
    private String storagePath;

    /** Small on purpose: these are marks, not photographs. */
    @Value("${company.asset.max-bytes:2097152}")
    private long maxBytes;

    private static final Map<String, String> ALLOWED = new LinkedHashMap<>();
    static {
        ALLOWED.put("svg", "image/svg+xml");
        ALLOWED.put("png", "image/png");
        ALLOWED.put("jpg", "image/jpeg");
        ALLOWED.put("jpeg", "image/jpeg");
        ALLOWED.put("webp", "image/webp");
        ALLOWED.put("ico", "image/x-icon");
    }

    /** What must not appear inside an uploaded SVG. */
    private static final String[] ACTIVE_CONTENT = {
        "<script", "javascript:", "onload=", "onerror=", "onclick=", "onmouseover=",
        "<foreignobject", "<iframe", "<embed", "<use xlink:href=\"http", "<animate"
    };

    // ------------------------------------------------------------------ upload

    @Transactional
    public ResponseEntity<ApiResponse<?>> upload(String kindPath, MultipartFile file) {
        CompanyAsset.AssetKind kind = parseKind(kindPath);
        if (kind == null) return badRequest("Unknown asset slot '" + kindPath + "'. Expected one of: " + slotNames());

        if (file == null || file.isEmpty()) return badRequest("No file was uploaded");
        if (file.getSize() > maxBytes) {
            return badRequest("That file is " + CompanyProfileGetService.formatFileSize(file.getSize())
                + ". A logo must be under " + CompanyProfileGetService.formatFileSize(maxBytes)
                + " — these files load on every page and every email.");
        }

        String original = file.getOriginalFilename() == null ? "upload" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = extensionOf(original);
        String mimeType = ALLOWED.get(extension);
        if (mimeType == null) {
            return badRequest("'" + extension + "' files are not accepted here. Use one of: " + String.join(", ", ALLOWED.keySet()));
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Could not read uploaded company asset '{}'", original, e);
            return serverError("The upload could not be read. Try again.");
        }

        if ("svg".equals(extension)) {
            String offending = activeContentIn(bytes);
            if (offending != null) {
                log.warn("Rejected SVG company asset '{}' — contains {}", original, offending);
                return badRequest("That SVG contains active content (" + offending
                    + "). Export it again as a plain vector, without scripts or embedded objects.");
            }
        }

        if (kind == CompanyAsset.AssetKind.LOGO_EMAIL && "svg".equals(extension)) {
            return badRequest("The email logo has to be a raster image — Outlook and Gmail do not render SVG. "
                + "Export the same logo as a PNG, around 400px wide.");
        }

        CompanyProfile profile = writeService.requireProfile();

        String storedName;
        try {
            storedName = write(kind, extension, bytes);
        } catch (IOException e) {
            log.error("Could not store company asset for slot {}", kind, e);
            return serverError("The file could not be saved to disk. Check the data directory.");
        }

        /*
         * One row per slot (uq_company_asset_kind), so an upload REPLACES rather than accumulates —
         * and the file the old row pointed at is deleted, or the data directory grows a copy of every
         * logo anybody ever tried.
         */
        Optional<CompanyAsset> existing = profile.getAssets().stream()
            .filter(a -> a.getAssetKind() == kind).findFirst();

        existing.ifPresent(old -> {
            if (old.getFileName() != null && !old.getFileName().equals(storedName)) delete(old.getFileName());
        });

        CompanyAsset asset = existing.orElseGet(() -> {
            CompanyAsset created = CompanyAsset.builder().companyProfile(profile).assetKind(kind).build();
            profile.getAssets().add(created);
            return created;
        });

        asset.setFileName(storedName);
        asset.setOriginalFileName(original);
        asset.setMimeType(mimeType);
        asset.setFileSize((long) bytes.length);
        asset.setIsActive(true);

        profileRepository.save(profile);
        identityService.invalidate();
        log.info("Company asset {} set from '{}' ({})", kind, original, CompanyProfileGetService.formatFileSize((long) bytes.length));

        return ResponseEntity.ok(ApiResponse.success(200, slotLabel(kind) + " updated successfully", payload(profile)));
    }

    // ------------------------------------------------------------------ delete

    @Transactional
    public ResponseEntity<ApiResponse<?>> remove(String kindPath) {
        CompanyAsset.AssetKind kind = parseKind(kindPath);
        if (kind == null) return badRequest("Unknown asset slot '" + kindPath + "'. Expected one of: " + slotNames());

        CompanyProfile profile = profileRepository.findSingleton().orElse(null);
        if (profile == null) return notFound(kind);

        Optional<CompanyAsset> existing = profile.getAssets().stream()
            .filter(a -> a.getAssetKind() == kind).findFirst();
        if (existing.isEmpty()) return notFound(kind);

        CompanyAsset asset = existing.get();
        delete(asset.getFileName());
        profile.getAssets().remove(asset);

        profileRepository.save(profile);
        identityService.invalidate();
        log.info("Company asset {} removed", kind);

        return ResponseEntity.ok(ApiResponse.success(200, slotLabel(kind) + " removed successfully", payload(profile)));
    }

    // ------------------------------------------------------------------ serve

    /**
     * Public, because an {@code <img>} in an email or on the website carries no bearer token — the
     * same reason the other media endpoints are public. A logo is published material by definition.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> serve(String kindPath) {
        CompanyAsset.AssetKind kind = parseKind(kindPath);
        if (kind == null) return ResponseEntity.notFound().build();

        CompanyProfile profile = profileRepository.findSingleton().orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();

        CompanyAsset asset = profile.getAssets().stream()
            .filter(a -> a.getAssetKind() == kind && Boolean.TRUE.equals(a.getIsActive()))
            .findFirst().orElse(null);
        if (asset == null || asset.getFileName() == null) return ResponseEntity.notFound().build();

        Path path = Paths.get(storagePath).resolve(asset.getFileName()).normalize();
        if (!path.startsWith(Paths.get(storagePath).normalize()) || !Files.exists(path)) {
            log.error("Company asset {} points at '{}', which is not on disk", kind, asset.getFileName());
            return ResponseEntity.notFound().build();
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("Could not read company asset {} from '{}'", kind, path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        MediaType mediaType = asset.getMimeType() == null
            ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(asset.getMimeType());

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + asset.getFileName() + "\"")
            /* an SVG served from this origin must not be able to fetch or run anything */
            .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
            .header("X-Content-Type-Options", "nosniff")
            /* the URL is stable per slot, so the file name is the version */
            .eTag("\"" + asset.getFileName() + "\"")
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
            .body(new ByteArrayResource(bytes));
    }

    // ------------------------------------------------------------------ internals

    private Map<String, Object> payload(CompanyProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("assets", getService.assetSlots(profile));
        map.put("completeness", getService.completeness(profile));
        return map;
    }

    private String write(CompanyAsset.AssetKind kind, String extension, byte[] bytes) throws IOException {
        Path dir = Paths.get(storagePath);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        String name = kind.name().toLowerCase(Locale.ROOT) + "-" + shortHash(bytes) + "." + extension;
        Path target = dir.resolve(name);
        Files.copy(new java.io.ByteArrayInputStream(bytes), target, StandardCopyOption.REPLACE_EXISTING);
        return name;
    }

    private void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) return;
        try {
            Path path = Paths.get(storagePath).resolve(fileName).normalize();
            if (path.startsWith(Paths.get(storagePath).normalize())) Files.deleteIfExists(path);
        } catch (IOException e) {
            /* a leftover file is untidy; failing the request over it would be worse */
            log.warn("Could not delete old company asset '{}': {}", fileName, e.getMessage());
        }
    }

    private String shortHash(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(bytes));
        }
    }

    private String activeContentIn(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        for (String marker : ACTIVE_CONTENT) {
            if (text.contains(marker)) return marker;
        }
        return null;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Accepts both {@code logo-light} (URL form) and {@code LOGO_LIGHT} (enum form). */
    public static CompanyAsset.AssetKind parseKind(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return CompanyAsset.AssetKind.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String slotNames() {
        return String.join(", ", java.util.Arrays.stream(CompanyAsset.AssetKind.values())
            .map(k -> k.name().toLowerCase(Locale.ROOT).replace('_', '-')).toList());
    }

    private String slotLabel(CompanyAsset.AssetKind kind) {
        List<CompanyAssetDTO> slots = getService.assetSlots(null);
        return slots.stream().filter(s -> kind.name().equals(s.getAssetKind()))
            .map(CompanyAssetDTO::getLabel).findFirst().orElse(kind.name());
    }

    private ResponseEntity<ApiResponse<?>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, message, "VALIDATION_ERROR"));
    }

    private ResponseEntity<ApiResponse<?>> notFound(CompanyAsset.AssetKind kind) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(404, "No file has been uploaded for " + slotLabel(kind), "NOT_FOUND"));
    }

    private ResponseEntity<ApiResponse<?>> serverError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, message, "STORAGE_ERROR"));
    }
}

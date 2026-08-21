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

        /*
         * An SVG in an email slot is now converted rather than refused.
         *
         * The rule was right — Gmail and Outlook draw a broken-image box for an SVG — but refusing
         * the upload left the person with a task ("go and export a PNG") standing between them and a
         * working letterhead, and a company whose designer only supplied vectors could not finish.
         * The endpoint rasterises on the way out, so the file is accepted and the email still gets a
         * raster.
         */

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

        CompanyAsset asset = resolveAsset(profile, kind);
        if (asset == null) return ResponseEntity.notFound().build();

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
        String servedName = asset.getFileName();

        /*
         * The email slot always answers with a raster, whatever it had to borrow. An <img> in an
         * email pointing at an SVG is a broken-image box in Gmail and in Outlook, and that box is
         * the first thing a customer sees.
         */
        if (isEmailSlot(kind) && "svg".equals(extensionOf(asset.getFileName()))) {
            try {
                bytes = rasterise(path, asset.getFileName());
                mediaType = MediaType.IMAGE_PNG;
                servedName = asset.getFileName().replaceAll("\\.svg$", "") + ".png";
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + servedName + "\"")
            /* an SVG served from this origin must not be able to fetch or run anything */
            .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
            .header("X-Content-Type-Options", "nosniff")
            /* the URL is stable per slot, so the file name is the version */
            .eTag("\"" + servedName + "\"")
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
            .body(new ByteArrayResource(bytes));
    }

    /**
     * What to serve when a slot is empty.
     *
     * A template refers to a slot by a fixed URL — /api/public/company/assets/logo-email — and that
     * URL has to answer for every installation, or the same template breaks on one company and not
     * on another. A 404 there is a broken-image box in somebody's invoice, so an empty slot borrows
     * the nearest logo the company HAS rather than admitting nothing.
     *
     * Light borrowing from dark (and back) is deliberate: a mark on the wrong background is a
     * cosmetic problem, and a missing one is not.
     */
    private static final Map<CompanyAsset.AssetKind, CompanyAsset.AssetKind[]> FALLBACKS = new LinkedHashMap<>();
    static {
        /* the dark-header copy borrows the DARK ink first — that is the whole point of the slot */
        FALLBACKS.put(CompanyAsset.AssetKind.LOGO_EMAIL_DARK, new CompanyAsset.AssetKind[] {
            CompanyAsset.AssetKind.LOGO_DARK, CompanyAsset.AssetKind.LOGO_EMAIL,
            CompanyAsset.AssetKind.LOGO_FULL, CompanyAsset.AssetKind.LOGO_LIGHT });
        FALLBACKS.put(CompanyAsset.AssetKind.LOGO_EMAIL, new CompanyAsset.AssetKind[] {
            CompanyAsset.AssetKind.LOGO_FULL, CompanyAsset.AssetKind.LOGO_FULL_TAGLINE,
            CompanyAsset.AssetKind.LOGO_LIGHT, CompanyAsset.AssetKind.LOGO_DARK });
        FALLBACKS.put(CompanyAsset.AssetKind.LOGO_FULL, new CompanyAsset.AssetKind[] {
            CompanyAsset.AssetKind.LOGO_FULL_TAGLINE, CompanyAsset.AssetKind.LOGO_LIGHT });
        FALLBACKS.put(CompanyAsset.AssetKind.LOGO_FULL_TAGLINE, new CompanyAsset.AssetKind[] {
            CompanyAsset.AssetKind.LOGO_FULL, CompanyAsset.AssetKind.LOGO_LIGHT });
        FALLBACKS.put(CompanyAsset.AssetKind.LOGO_LIGHT, new CompanyAsset.AssetKind[] {
            CompanyAsset.AssetKind.LOGO_FULL, CompanyAsset.AssetKind.LOGO_DARK });
        FALLBACKS.put(CompanyAsset.AssetKind.LOGO_DARK, new CompanyAsset.AssetKind[] {
            CompanyAsset.AssetKind.LOGO_FULL, CompanyAsset.AssetKind.LOGO_LIGHT });
        FALLBACKS.put(CompanyAsset.AssetKind.FAVICON_LIGHT, new CompanyAsset.AssetKind[] {
            CompanyAsset.AssetKind.FAVICON_DARK, CompanyAsset.AssetKind.LOGO_LIGHT });
        FALLBACKS.put(CompanyAsset.AssetKind.FAVICON_DARK, new CompanyAsset.AssetKind[] {
            CompanyAsset.AssetKind.FAVICON_LIGHT, CompanyAsset.AssetKind.LOGO_DARK });
    }

    private static boolean isEmailSlot(CompanyAsset.AssetKind kind) {
        return kind == CompanyAsset.AssetKind.LOGO_EMAIL || kind == CompanyAsset.AssetKind.LOGO_EMAIL_DARK;
    }

    /** The uploaded file for a slot, or the nearest stand-in. */
    private CompanyAsset resolveAsset(CompanyProfile profile, CompanyAsset.AssetKind kind) {
        CompanyAsset own = active(profile, kind);
        if (own != null) return own;
        for (CompanyAsset.AssetKind alternative : FALLBACKS.getOrDefault(kind, new CompanyAsset.AssetKind[0])) {
            CompanyAsset borrowed = active(profile, alternative);
            if (borrowed != null) return borrowed;
        }
        return null;
    }

    private CompanyAsset active(CompanyProfile profile, CompanyAsset.AssetKind kind) {
        return profile.getAssets().stream()
            .filter(a -> a.getAssetKind() == kind && Boolean.TRUE.equals(a.getIsActive()))
            .filter(a -> a.getFileName() != null)
            .findFirst().orElse(null);
    }

    /**
     * A PNG of an SVG, for the email slot.
     *
     * Mail clients do not render SVG — Gmail and Outlook both show a broken-image box — which is why
     * uploading an SVG into the email slot is refused. But a company that has only uploaded vector
     * logos still has to get a logo into its email, and telling them to export a PNG is a task
     * standing between them and a working letterhead. Batik is already on the classpath for the PDF
     * renderer, so the conversion happens here instead.
     *
     * Cached beside the original and keyed by its file name, which already contains a content hash,
     * so a re-upload produces a different name and cannot be served a stale raster.
     */
    private byte[] rasterise(Path svg, String cacheKey) throws IOException {
        Path cache = Paths.get(storagePath).resolve("derived").resolve(cacheKey + ".png");
        if (Files.exists(cache)) return Files.readAllBytes(cache);

        try {
            org.apache.batik.transcoder.image.PNGTranscoder transcoder =
                new org.apache.batik.transcoder.image.PNGTranscoder();
            /* wide enough for a retina letterhead; height follows the aspect ratio */
            transcoder.addTranscodingHint(
                org.apache.batik.transcoder.image.PNGTranscoder.KEY_WIDTH, 600f);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream in = Files.newInputStream(svg)) {
                transcoder.transcode(
                    new org.apache.batik.transcoder.TranscoderInput(in),
                    new org.apache.batik.transcoder.TranscoderOutput(out));
            }
            byte[] png = out.toByteArray();
            Files.createDirectories(cache.getParent());
            Files.write(cache, png);
            log.info("Rasterised company logo '{}' to PNG for email ({} bytes)", svg.getFileName(), png.length);
            return png;
        } catch (org.apache.batik.transcoder.TranscoderException e) {
            /* a logo the converter chokes on is not a reason to fail the request outright */
            log.error("Could not rasterise company logo '{}' for email", svg.getFileName(), e);
            throw new IOException("SVG could not be converted to PNG", e);
        }
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

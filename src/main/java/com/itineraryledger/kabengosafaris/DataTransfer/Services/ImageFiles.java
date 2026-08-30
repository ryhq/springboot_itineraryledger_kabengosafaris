package com.itineraryledger.kabengosafaris.DataTransfer.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.DataTransfer.Scalars;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferFile;

import lombok.extern.slf4j.Slf4j;

/**
 * The picture files, when somebody asks for them.
 *
 * Off by default because they dominate the bundle: a lodge's gallery is megabytes and its rate sheet
 * is kilobytes, and the usual reason to move data between companies is the rates. When they are
 * wanted, the row travels in the module's JSON like anything else and the FILE travels beside it
 * under a name the bundle chooses — never the absolute path it had, which belongs to one machine,
 * one service account and one company's data directory.
 *
 * A file that has gone missing from disk is skipped with a line in the log rather than failing the
 * export. A gallery with a hole in it is a nuisance; an export that refuses to produce anything
 * because of one lost thumbnail is worse.
 */
@Service
@Slf4j
public class ImageFiles {

    @Value("${park.image.storage.path:./data/park-images/}")
    private String parkImages;

    @Value("${accommodation.image.storage.path:./data/accommodation-images/}")
    private String accommodationImages;

    @Value("${activity.image.storage.path:./data/activity-images/}")
    private String activityImages;

    public String rootFor(String module) {
        return switch (module) {
            case "parks" -> parkImages;
            case "accommodations" -> accommodationImages;
            case "activities" -> activityImages;
            default -> null;
        };
    }

    /**
     * Add an owner's images to its JSON node, and collect the files to be carried.
     *
     * @param collected files to add to the bundle; left untouched when the caller did not ask for them
     */
    public void attach(ObjectMapper mapper, ObjectNode owner, String module, String ownerKey,
                       List<?> images, boolean includeImages, List<TransferFile> collected) {
        if (!includeImages || images == null || images.isEmpty()) return;

        String root = rootFor(module);
        if (root == null) return;

        ArrayNode rows = owner.putArray("images");
        for (Object image : images) {
            ObjectNode row = Scalars.of(mapper, image);
            String fileName = row.path("fileName").asText(null);
            if (fileName == null) continue;

            Path source = Paths.get(root).resolve(fileName).normalize();
            if (!Files.exists(source)) {
                log.warn("{} image '{}' is recorded but not on disk — leaving it out of the bundle",
                    module, fileName);
                continue;
            }
            String inBundle = "files/" + module + "/" + ownerKey + "/" + fileName;
            row.put("file", inBundle);
            collected.add(new TransferFile(inBundle, source));
            rows.add(row);
        }
    }

    /**
     * Put one image file where this installation keeps them, and say what it ended up called.
     *
     * Returns null when the bundle did not carry the file, which is the normal case for a bundle
     * exported without images: the row is then not worth writing either, since an image record
     * pointing at a file that is not there renders as a broken box.
     */
    public String place(TransferContext context, String module, JsonNode imageRow) {
        if (context.getFiles() == null) return null;

        String inBundle = imageRow.path("file").asText(null);
        String fileName = imageRow.path("fileName").asText(null);
        if (inBundle == null || fileName == null) return null;

        Path source = context.getFiles().resolve(inBundle).normalize();
        if (!source.startsWith(context.getFiles())) {
            /* a bundle is somebody else's zip; a path climbing out of it is not a mistake */
            log.error("Refused an image path that leaves the bundle: {}", inBundle);
            return null;
        }
        if (!java.nio.file.Files.exists(source)) return null;

        try {
            String root = rootFor(module);
            if (root == null) return null;
            Path target = Paths.get(root).resolve(fileName).normalize();
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (Exception e) {
            log.error("Could not place {} image '{}'", module, fileName, e);
            return null;
        }
    }

    /** Somewhere to keep the files an export is collecting, for the caller to zip. */
    public static List<TransferFile> collector() {
        return new ArrayList<>();
    }
}

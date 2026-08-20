package com.itineraryledger.kabengosafaris.Security;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * That every permission an endpoint demands actually exists.
 *
 * A permission is matched by NAME. Demand one the catalogue does not contain and the endpoint answers
 * 403 to everybody — including a superadmin, who has every permission there is, because the one being
 * asked for is not among them. Nothing fails at startup and nothing appears in a log; somebody simply
 * cannot use a page and nobody can say why.
 *
 * This is not hypothetical: a bulk-restore endpoint written minutes before this test asked for
 * PERM_UPDATE_EMAIL_SIGNATURE, while the catalogue calls that entity EMAIL_ACCOUNT_SIGNATURE.
 */
class PermissionNamesTest {

    private static final Path JAVA = Paths.get("src/main/java/com/itineraryledger/kabengosafaris");
    private static final Path ENTITIES = Paths.get("src/main/resources/permissions/entities.json");
    private static final Path CUSTOM = Paths.get("src/main/resources/permissions/custom-permissions.json");

    @Test
    @DisplayName("no endpoint asks for a permission the catalogue cannot grant")
    void everyDemandedPermissionExists() throws IOException {
        Set<String> known = knownPermissionNames();
        Pattern demanded = Pattern.compile("PERM_([A-Z0-9_]+)");
        List<String> unknown = new ArrayList<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (!line.contains("hasAuthority")) continue;
                    /*
                     * A comment explaining the pattern is not a demand. GlobalExceptionHandler parses
                     * "hasAuthority('PERM_XXX')" out of Spring's own message to tell the user which
                     * permission they lack, and its example is not a real name.
                     */
                    String trimmed = line.trim();
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) continue;
                    Matcher m = demanded.matcher(line);
                    while (m.find()) {
                        if (!known.contains(m.group(1))) {
                            unknown.add(JAVA.relativize(file) + ":" + (i + 1) + "  PERM_" + m.group(1));
                        }
                    }
                }
            }
        }

        assertTrue(unknown.isEmpty(), () -> """
            %d endpoint(s) demand a permission that does not exist, so they answer 403 to everybody
            including SUPERADMIN:

            %s

            Either the name is wrong (check what entities.json calls that entity — EMAIL_SIGNATURE
            against EMAIL_ACCOUNT_SIGNATURE, for instance) or the permission needs adding to
            entities.json or custom-permissions.json.
            """.formatted(unknown.size(), String.join("\n", unknown)));
    }

    private Set<String> knownPermissionNames() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Set<String> names = new TreeSet<>();

        JsonNode entities = mapper.readTree(Files.readString(ENTITIES)).get("entities");
        for (JsonNode entity : entities) {
            for (String action : List.of("CREATE", "READ", "UPDATE", "DELETE")) {
                names.add(action + "_" + entity.asText());
            }
        }

        JsonNode custom = mapper.readTree(Files.readString(CUSTOM)).get("customPermissions");
        for (JsonNode permission : custom) names.add(permission.get("name").asText());

        assertFalse(names.isEmpty(), "no permissions parsed — this guard would be checking nothing");
        return new HashSet<>(names);
    }
}

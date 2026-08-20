package com.itineraryledger.kabengosafaris.CompanyProfile.Services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Adds the company's variables to whatever a record declares, when it is read.
 *
 * At READ time rather than in the seed, deliberately. Seeded rows are never overwritten — that is
 * what protects somebody's edits — so a variable added to a seed would only ever appear on
 * installations created afterwards. Merging here means every event, document and signature, on every
 * installation, gains the company variables the moment this ships, and the next one added needs no
 * migration either.
 *
 * The record's own declarations win: if an event already declares `companyEmail` with a default, that
 * definition is left exactly as it is.
 */
@Slf4j
public final class CompanyVariableMerger {

    private CompanyVariableMerger() {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** For an email event or a signature: variables are flat `{{name}}` placeholders. */
    public static String mergeEmailVariables(String variablesJson) {
        return merge(variablesJson, CompanyVariableCatalogue.asEmailVariables(), "name");
    }

    /** For a PDF document: variables are `${path}` expressions into the model. */
    public static String mergePdfVariables(String variablesJson) {
        return merge(variablesJson, CompanyVariableCatalogue.asPdfVariables(), "path");
    }

    private static String merge(String variablesJson, List<Map<String, Object>> additions, String key) {
        List<Map<String, Object>> declared = new ArrayList<>();

        if (variablesJson != null && !variablesJson.isBlank()) {
            try {
                declared = MAPPER.readValue(variablesJson, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                /*
                 * A schema that cannot be parsed is a hint that cannot be shown, not a reason to fail
                 * the request the panel is making. Log it and offer the company variables alone.
                 */
                log.warn("Could not parse a declared variable list; offering the company variables only: {}",
                    e.getMessage());
            }
        }

        Set<String> already = new HashSet<>();
        for (Map<String, Object> row : declared) {
            Object name = row.get(key) != null ? row.get(key) : row.get("name");
            if (name != null) already.add(String.valueOf(name));
        }

        for (Map<String, Object> addition : additions) {
            if (!already.contains(String.valueOf(addition.get(key)))) declared.add(addition);
        }

        try {
            return MAPPER.writeValueAsString(declared);
        } catch (Exception e) {
            log.error("Could not write the merged variable list", e);
            return variablesJson;
        }
    }
}

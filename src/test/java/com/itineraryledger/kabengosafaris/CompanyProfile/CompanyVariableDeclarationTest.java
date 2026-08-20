package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyVariableCatalogue;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyVariableMerger;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.SignatureVariable;

/**
 * That every company variable a panel OFFERS is one the renderers actually fill.
 *
 * The bug this closes: the variables were substituted everywhere and declared nowhere, so the
 * "variables you can use" panel listed only the record's own fields and template authors typed the
 * company's details by hand — which is the drift the whole company profile exists to end.
 *
 * The opposite mistake is worse: offering a variable nothing fills puts literal braces in front of a
 * customer. So the catalogue is checked against what the identity actually produces.
 */
class CompanyVariableDeclarationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("every offered variable is one the identity supplies")
    void catalogueMatchesWhatIsSupplied() {
        Map<String, String> supplied = CompanyIdentityService.Snapshot.empty("x").variables();

        List<String> offeredButUnfilled = CompanyVariableCatalogue.variables().keySet().stream()
            .filter(name -> !supplied.containsKey(name))
            /* added per render rather than by the snapshot */
            .filter(name -> !name.equals("currentYear"))
            .toList();

        assertTrue(offeredButUnfilled.isEmpty(),
            "these are offered to template authors but nothing fills them, so they would reach a "
                + "customer as literal braces: " + offeredButUnfilled);
    }

    @Test
    @DisplayName("an event that declared nothing about the company now declares all of it")
    void mergedIntoAnEmailEvent() throws Exception {
        String declared = """
            [{"name":"safariCode","description":"Safari reference","isRequired":true}]""";

        String merged = CompanyVariableMerger.mergeEmailVariables(declared);
        List<Map<String, Object>> rows = MAPPER.readValue(merged, new TypeReference<>() {});

        assertEquals("safariCode", rows.get(0).get("name"), "the event's own variables come first");
        assertTrue(rows.stream().anyMatch(r -> "companyName".equals(r.get("name"))));
        assertTrue(rows.stream().anyMatch(r -> "companyAccent".equals(r.get("name"))),
            "the brand colour is a variable too — a template that hardcodes a green is as wrong as "
                + "one that hardcodes a name");
        assertTrue(rows.stream().anyMatch(r -> "companyLogoFullUrl".equals(r.get("name"))));
    }

    @Test
    @DisplayName("a declaration the event already made is left exactly as it was")
    void ownDeclarationsWin() throws Exception {
        String declared = """
            [{"name":"companyEmail","description":"mine","isRequired":true,"defaultValue":"x@y.z"}]""";

        List<Map<String, Object>> rows =
            MAPPER.readValue(CompanyVariableMerger.mergeEmailVariables(declared), new TypeReference<>() {});

        Map<String, Object> companyEmail = rows.stream()
            .filter(r -> "companyEmail".equals(r.get("name"))).findFirst().orElseThrow();
        assertEquals("mine", companyEmail.get("description"), "the record's own definition wins");
        assertEquals(1, rows.stream().filter(r -> "companyEmail".equals(r.get("name"))).count(),
            "and it is not declared twice");
    }

    @Test
    @DisplayName("a PDF document declares paths into the model, not flat names")
    void mergedIntoAPdfDocument() throws Exception {
        List<Map<String, Object>> rows = MAPPER.readValue(
            CompanyVariableMerger.mergePdfVariables("[{\"path\":\"safari.id\",\"type\":\"string\"}]"),
            new TypeReference<>() {});

        assertTrue(rows.stream().anyMatch(r -> "company.name".equals(r.get("path"))));
        assertTrue(rows.stream().anyMatch(r -> "company.bank.swift".equals(r.get("path"))));
        assertTrue(rows.stream().anyMatch(r -> "company.documentLogoUrl".equals(r.get("path"))));
        assertTrue(rows.stream().noneMatch(r -> "companyName".equals(r.get("path"))),
            "a Thymeleaf template reads a path; a flat name would silently render nothing");
    }

    @Test
    @DisplayName("the merged list still deserialises into the signature's own model")
    void signaturesCanReadIt() throws Exception {
        /*
         * The signature model has three fields and the merged rows carry five. Jackson fails on
         * unknown properties by default, and that failure is caught and turned into an empty list —
         * which would have silently hidden a signature's own variables as well as the company's.
         */
        String merged = CompanyVariableMerger.mergeEmailVariables("[]");
        List<SignatureVariable> parsed = MAPPER.readValue(merged,
            MAPPER.getTypeFactory().constructCollectionType(List.class, SignatureVariable.class));

        assertFalse(parsed.isEmpty());
        assertTrue(parsed.stream().anyMatch(v -> "companyName".equals(v.getName())));
    }
}

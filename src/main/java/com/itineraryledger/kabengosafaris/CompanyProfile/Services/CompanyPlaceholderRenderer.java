package com.itineraryledger.kabengosafaris.CompanyProfile.Services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Fills in the company's `{{placeholders}}` wherever they appear.
 *
 * One implementation, because there are two renderers and they must agree. The email templates had
 * this pass; signatures did not — signatures substitute `{name}` for the variables they declare, so a
 * `{{companyName}}` in a signature went out to the customer as those exact characters. Whatever
 * carries the company's details has to resolve them the same way.
 *
 * Optional blocks are supported: `{{#companyInstagram}}…{{/companyInstagram}}` keeps its contents only
 * when the value is set, so a company with no Instagram page ships no icon linking nowhere.
 */
@Component
@RequiredArgsConstructor
public class CompanyPlaceholderRenderer {

    private final CompanyIdentityService companyIdentityService;

    /** The company's variables plus the year, which is not part of the cached identity. */
    public Map<String, String> variables() {
        Map<String, String> map = new LinkedHashMap<>(companyIdentityService.variables());
        map.put("currentYear", String.valueOf(java.time.Year.now().getValue()));
        return map;
    }

    /**
     * Resolves every company placeholder in the given HTML, leaving everything else untouched.
     *
     * Safe to run after another pass: it only replaces what it recognises, so a caller's own values
     * (a preview, a test send) that already substituted a name are not disturbed.
     */
    public String apply(String html) {
        if (html == null || html.isEmpty()) return html;

        String result = html;
        for (Map.Entry<String, String> entry : variables().entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();

            String open = "{{#" + name + "}}";
            if (result.contains(open)) {
                Matcher sections = Pattern.compile(
                    "\\{\\{#" + Pattern.quote(name) + "\\}\\}(.*?)\\{\\{/" + Pattern.quote(name) + "\\}\\}",
                    Pattern.DOTALL).matcher(result);
                result = value.isBlank() ? sections.replaceAll("") : sections.replaceAll("$1");
            }

            String placeholder = "{{" + name + "}}";
            if (result.contains(placeholder)) result = result.replace(placeholder, value);
        }
        return result;
    }
}

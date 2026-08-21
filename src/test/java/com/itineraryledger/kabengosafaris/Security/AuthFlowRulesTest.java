package com.itineraryledger.kabengosafaris.Security;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rules the sign-in flows have to keep, checked at the source since the alternative is a live
 * account and a live mailbox.
 *
 * Each one is here because it was broken and the break was invisible:
 *
 *  · A reset link is a stateless JWT, so it worked over and over for its whole lifetime — including
 *    after its owner had already reset. Nothing in the code said otherwise; there was simply no check.
 *  · An address was written to the users table before the activation email went out, and a second
 *    attempt was refused, so filling in a form permanently claimed somebody else's address.
 *  · /api/company/variables was reachable with `isAuthenticated()` while it carried the TIN and the
 *    bank account — and on this product authentication is self-service, so that meant anybody.
 */
class AuthFlowRulesTest {

    private static final Path JAVA = Path.of("src/main/java");

    private String read(String relative) throws IOException {
        return Files.readString(JAVA.resolve(relative));
    }

    @Test
    @DisplayName("a password change stamps the account, so outstanding reset links are spent")
    void everyPasswordChangeIsStamped() throws IOException {
        List<String> unstamped = new ArrayList<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                /*
                 * Only the USER password matters. Email accounts call setPassword on a mail sender,
                 * which has nothing to do with anybody signing in.
                 */
                if (!source.contains("PasswordHasher.hashPassword(")) continue;
                if (!source.contains("setPasswordChangedAt(") && !source.contains("passwordChangedAt(")) {
                    unstamped.add(JAVA.relativize(file).toString());
                }
            }
        }

        assertTrue(unstamped.isEmpty(),
            "These set a user's password without stamping passwordChangedAt, so a reset link issued "
                + "beforehand stays usable afterwards: " + unstamped);
    }

    @Test
    @DisplayName("resetting compares the link's issued-at against the stamp")
    void theResetChecksTheStamp() throws IOException {
        String source = read("com/itineraryledger/kabengosafaris/User/Services/"
            + "PasswordResetServices/PasswordResetService.java");

        assertTrue(source.contains("getIssuedAt(token)"),
            "without the token's issued-at there is nothing to compare, and the link never expires "
                + "on use");
        assertTrue(source.contains("getPasswordChangedAt()"),
            "the stamp is what makes a used link spent");
        /* the same wording for spent and expired: the difference tells a stranger they were first */
        assertFalse(source.contains("already been used"),
            "saying a link was already used confirms to whoever holds it that somebody got there "
                + "first — which is information about the account holder, not about the link");
    }

    @Test
    @DisplayName("an unactivated registration can be replaced, an activated account cannot")
    void anUnprovedAddressIsNotOwned() throws IOException {
        String source = read("com/itineraryledger/kabengosafaris/User/Services/"
            + "RegistrationServices/RegistrationServices.java");

        assertTrue(source.contains("isUnclaimed("),
            "a pending registration has to be distinguishable from a real account");
        /*
         * Both halves of the test, because `enabled == false` alone also describes an account an
         * administrator deactivated — and handing that address to the next person who asks would be
         * account takeover by web form.
         */
        int at = source.indexOf("private boolean isUnclaimed");
        String predicate = source.substring(at, source.indexOf('}', at));
        assertTrue(predicate.contains("getEnabled()"), "must require never-activated: " + predicate);
        assertTrue(predicate.contains("getRoles()"),
            "must also require no roles, or a deactivated staff account could be claimed: " + predicate);
    }

    /**
     * The endpoints that may be guarded by authentication alone, and nothing more.
     *
     * Deliberately a list rather than a rule about names. Registration is self-service on this
     * product — anyone can create an account from the sign-in screen and activate it from their own
     * inbox — so `isAuthenticated()` means "can receive email", not "works here". Every entry here is
     * either about the caller's own account or is a fact the sign-in screen needs anyway.
     */
    private static final List<String> MAY_BE_AUTHENTICATED_ONLY = List.of(
        "getCurrentUser",          // your own profile
        "getCurrentUserRoles",     // your own roles
        "updatePersonalDetails",   // your own name and phone
        "updatePassword",          // your own password
        "getFeatures");            // which pages exist; the panel cannot render a nav without it

    @Test
    @DisplayName("only self-service endpoints are guarded by authentication alone")
    void authenticationAloneIsNotAStaffCheck() throws IOException {
        List<String> unexpected = new ArrayList<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith("Controller.java")).toList()) {
                String source = Files.readString(file);
                int at = source.indexOf("@PreAuthorize(\"isAuthenticated()\")");
                while (at >= 0) {
                    /* the first method declared after the annotation is the one it guards */
                    java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("public\\s+[\\w<>?,\\[\\]\\s.]+?\\s(\\w+)\\s*\\(")
                        .matcher(source.substring(at));
                    String method = m.find() ? m.group(1) : "(unknown)";
                    if (!MAY_BE_AUTHENTICATED_ONLY.contains(method)) {
                        unexpected.add(JAVA.relativize(file).getFileName() + "#" + method);
                    }
                    at = source.indexOf("@PreAuthorize(\"isAuthenticated()\")", at + 1);
                }
            }
        }

        assertTrue(unexpected.isEmpty(),
            "These are readable by anybody who can receive email, because registration is "
                + "self-service. Gate them on a permission, or add them to "
                + "MAY_BE_AUTHENTICATED_ONLY with a reason: " + unexpected);
    }

    @Test
    @DisplayName("the company variables endpoint needs a real permission, and hides money without one")
    void companyVariablesAreGated() throws IOException {
        String controller = read("com/itineraryledger/kabengosafaris/CompanyProfile/"
            + "Controller/CompanyProfileController.java");
        int at = controller.indexOf("getVariables");
        String guard = controller.substring(Math.max(0, at - 600), at);
        assertTrue(guard.contains("hasAnyAuthority"),
            "it carries the TIN and the default bank account, so authentication alone is not enough");

        String service = read("com/itineraryledger/kabengosafaris/CompanyProfile/"
            + "Services/CompanyProfileGetService.java");
        assertTrue(service.contains("NEEDS_COMPANY_READ"),
            "a template author may see the colours and the address; the account number is not part "
                + "of laying out a page");
        for (String secret : new String[] { "bankAccountNumber", "bankIban", "bankSwift", "companyTin" }) {
            assertTrue(service.contains("\"" + secret + "\""),
                secret + " must be in the redaction set: " + service.contains(secret));
        }
    }
}

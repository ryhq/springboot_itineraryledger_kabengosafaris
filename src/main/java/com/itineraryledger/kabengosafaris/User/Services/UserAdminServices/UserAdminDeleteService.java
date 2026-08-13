package com.itineraryledger.kabengosafaris.User.Services.UserAdminServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Removing an account, when that is actually the right thing to do.
 *
 * It usually is not. Almost every money record in this system carries the account
 * that created it, updated it or recorded the payment, and those stamps are the only
 * answer to "who did this" — an auditor's first question. Deleting the account behind
 * them either fails on a foreign key or, worse, succeeds and quietly empties the
 * column. So a delete is refused whenever anything points at the account, with the
 * counts that refused it, and deactivating is offered instead: the login stops
 * working and every stamp still names a person.
 *
 * What genuinely can be deleted is the account that never did anything — a typo in an
 * invite, a colleague who was set up twice.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminDeleteService {

    /**
     * Everywhere an account is recorded as having done something.
     *
     * Kept as data rather than a dozen repository methods because it is a list that
     * grows every time a module starts stamping its records, and a missed entry here
     * is a delete that fails with a database error instead of a sentence.
     */
    private static final List<Reference> REFERENCES = List.of(
        new Reference("Invoice", "createdBy", "invoice"),
        new Reference("Invoice", "updatedBy", "invoice"),
        new Reference("Payment", "recordedBy", "payment received"),
        new Reference("Quote", "createdBy", "quote"),
        new Reference("Quote", "updatedBy", "quote"),
        new Reference("Quote", "approvedBy", "quote"),
        new Reference("Quote", "approver", "quote"),
        new Reference("Safari", "createdBy", "safari"),
        new Reference("Safari", "updatedBy", "safari"),
        new Reference("CreditNote", "createdBy", "credit note"),
        new Reference("CreditNote", "updatedBy", "credit note"),
        new Reference("Expense", "createdBy", "bill"),
        new Reference("Expense", "updatedBy", "bill"),
        new Reference("ExpensePayment", "recordedBy", "payment made"),
        new Reference("BankAccount", "createdBy", "bank account"),
        new Reference("BankAccount", "updatedBy", "bank account"),
        new Reference("Vendor", "createdBy", "vendor"),
        new Reference("Vendor", "updatedBy", "vendor"),
        new Reference("Hero", "createdBy", "hero"),
        new Reference("Hero", "updatedBy", "hero"),
        new Reference("Testimony", "createdBy", "testimony"),
        new Reference("Testimony", "updatedBy", "testimony"));

    private record Reference(String entity, String field, String label) {}

    private static final String SUPERADMIN = "SUPERADMIN";

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final IdObfuscator idObfuscator;

    /** One id or fifty; the report shape is the same either way. */
    @Transactional
    public ResponseEntity<ApiResponse<?>> delete(List<String> idList, User actor) {
        if (idList == null || idList.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No ids supplied", "NO_IDS"));
        }

        List<String> deletedIds = new ArrayList<>();
        List<Map<String, String>> skipped = new ArrayList<>();

        for (String obfuscated : idList) {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscated);
            } catch (Exception e) {
                skipped.add(reason(obfuscated, null, "Unreadable id"));
                continue;
            }

            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                skipped.add(reason(obfuscated, null, "No longer exists"));
                continue;
            }

            if (actor != null && actor.getId().equals(id)) {
                skipped.add(reason(obfuscated, user.getUsername(),
                    "You cannot delete your own account"));
                continue;
            }

            if (isLastWayIn(user)) {
                skipped.add(reason(obfuscated, user.getUsername(),
                    "The last active " + SUPERADMIN + " — nobody could administer the system afterwards"));
                continue;
            }

            String blocking = describeReferences(id);
            if (blocking != null) {
                skipped.add(reason(obfuscated, user.getUsername(),
                    "Recorded against " + blocking + " — deactivate instead to keep the history"));
                continue;
            }

            try {
                /*
                 * Email-account grants are access, not history — the same thing being
                 * withdrawn here — so they go with the account rather than blocking it.
                 */
                entityManager.createQuery(
                    "delete from EmailAccountPermission x where x.user.id = :id")
                    .setParameter("id", id)
                    .executeUpdate();

                // roles are a join table; clearing first keeps the FK happy
                user.getRoles().clear();
                userRepository.save(user);
                userRepository.delete(user);
                deletedIds.add(obfuscated);
                log.info("Deleted account {}", user.getUsername());
            } catch (Exception e) {
                log.warn("Could not delete account {}", user.getUsername(), e);
                skipped.add(reason(obfuscated, user.getUsername(),
                    "Still referenced elsewhere — deactivate instead"));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", deletedIds.size());
        data.put("deletedIds", deletedIds);
        data.put("skipped", skipped);

        String message = deletedIds.size() + " account" + (deletedIds.size() == 1 ? "" : "s") + " deleted"
            + (skipped.isEmpty() ? "" : ", " + skipped.size() + " kept");
        return ResponseEntity.ok(ApiResponse.success(200, message, data));
    }

    /**
     * What points at this account, as a phrase a person can act on.
     *
     * Returns null when nothing does. Counts are grouped by what the reader would call
     * the thing — "3 invoices" rather than one line for created_by and another for
     * updated_by, which describes our schema instead of their problem.
     */
    private String describeReferences(Long userId) {
        Map<String, Long> counts = new LinkedHashMap<>();

        for (Reference reference : REFERENCES) {
            long count = count(reference, userId);
            if (count > 0) counts.merge(reference.label(), count, Long::sum);
        }

        if (counts.isEmpty()) return null;

        List<String> parts = new ArrayList<>();
        counts.forEach((label, count) ->
            parts.add(count + " " + label + (count == 1 ? "" : label.endsWith("s") ? "" : "s")));
        return String.join(", ", parts);
    }

    private long count(Reference reference, Long userId) {
        try {
            Long count = entityManager.createQuery(
                    "select count(x) from " + reference.entity() + " x where x." + reference.field() + ".id = :id",
                    Long.class)
                .setParameter("id", userId)
                .getSingleResult();
            return count == null ? 0 : count;
        } catch (Exception e) {
            /*
             * A renamed field or entity must not read as "nothing references this" —
             * that is how a delete gets through and empties an audit column. Report it
             * as a reference so the delete is refused, and log loudly.
             */
            log.error("Reference check {}.{} failed; treating the account as referenced",
                reference.entity(), reference.field(), e);
            return 1;
        }
    }

    /** Would deleting this account leave nobody able to administer the system? */
    private boolean isLastWayIn(User candidate) {
        if (!holdsSuperadmin(candidate)) return false;
        return userRepository.findAll().stream()
            .filter(other -> !other.getId().equals(candidate.getId()))
            .filter(other -> Boolean.TRUE.equals(other.getEnabled()))
            .noneMatch(this::holdsSuperadmin);
    }

    private boolean holdsSuperadmin(User user) {
        if (user.getRoles() == null) return false;
        return user.getRoles().stream()
            .anyMatch(role -> Boolean.TRUE.equals(role.getActive())
                && SUPERADMIN.equalsIgnoreCase(role.getName()));
    }

    private Map<String, String> reason(String id, String username, String reason) {
        Map<String, String> entry = new HashMap<>();
        entry.put("id", id);
        if (username != null) entry.put("code", username);
        entry.put("reason", reason);
        return entry;
    }

    /** For the record page, so the ⋯ menu can explain itself before anything is clicked. */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> checkDeletable(String obfuscated) {
        try {
            Long id = idObfuscator.decodeId(obfuscated);
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "User not found", "USER_NOT_FOUND"));
            }

            String blocking = describeReferences(id);
            Map<String, Object> data = new HashMap<>();
            data.put("deletable", blocking == null && !isLastWayIn(user));
            data.put("references", blocking);
            data.put("lastSuperadmin", isLastWayIn(user));
            return ResponseEntity.ok(ApiResponse.success(200, "Checked", data));
        } catch (Exception e) {
            log.error("Error checking whether {} can be deleted", obfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to check the account", "USER_CHECK_FAILED"));
        }
    }
}

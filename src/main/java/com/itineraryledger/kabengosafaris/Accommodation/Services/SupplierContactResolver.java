package com.itineraryledger.kabengosafaris.Accommodation.Services;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Who to write to at a property, and who to copy.
 *
 * <p>One address goes in the To and every other one is copied, which is what an office expects: a
 * lodge with a reservations desk and a general inbox should not have to be told twice, and the
 * person who answers should be able to see who else has it.
 *
 * <p>Extracted from the availability letter, which had all of this and kept it private. The payment
 * advice needed the same rules with a different preference — a lodge that has an accounts address
 * would rather hear about money there than at the reservations desk — and copying sixty lines of
 * parent-group fallback, de-duplication and ordering would have guaranteed the two drifted apart.
 */
@Service
public class SupplierContactResolver {

    /**
     * @param to         the one address the letter is addressed to, or null when the property has none
     * @param cc         everyone else, the property's own addresses before its group's
     * @param viaParent  true when nothing was found on the property and its group answered instead
     * @param parentName that group's name, for a line saying so
     * @param label      what the chosen address is called, e.g. "Reservations"
     */
    public record Recipients(String to, List<String> cc, boolean viaParent, String parentName, String label) {}

    /** Asking about rooms: the reservations desk, if there is one. */
    public Recipients forReservations(Accommodation property) {
        return resolve(property, List.of(AccommodationEmail.EmailType.RESERVATIONS));
    }

    /**
     * Telling them about money: accounts first, then billing, then whatever is primary.
     *
     * A payment advice sent to a reservations desk usually still arrives — somebody forwards it —
     * but a property that has told us where to send remittances has told us for a reason.
     */
    public Recipients forBilling(Accommodation property) {
        return resolve(property, List.of(
            AccommodationEmail.EmailType.BILLING,
            AccommodationEmail.EmailType.MANAGEMENT));
    }

    /**
     * The property's addresses, then its group's.
     *
     * A branch often keeps none of its own — everything for it is answered by the group — so a To
     * taken from the property alone reports a camp as unreachable when its group can be written to
     * today. Sibling branches are left out: another camp has no part in this.
     */
    private Recipients resolve(Accommodation property, List<AccommodationEmail.EmailType> preferred) {
        List<AccommodationEmail> mine = active(property);
        Accommodation parent = property.getParentAccommodation();
        List<AccommodationEmail> theirs = parent != null ? active(parent) : List.of();

        AccommodationEmail own = best(mine, preferred);
        AccommodationEmail chosen = own != null ? own : best(theirs, preferred);

        List<String> cc = new ArrayList<>();
        Set<String> seen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (chosen != null && chosen.getEmail() != null) seen.add(chosen.getEmail());
        for (List<AccommodationEmail> list : List.of(mine, theirs)) {
            for (AccommodationEmail email : list) {
                if (email.getEmail() == null || seen.contains(email.getEmail())) continue;
                seen.add(email.getEmail());
                cc.add(email.getEmail());
            }
        }

        return new Recipients(
            chosen != null ? chosen.getEmail() : null,
            cc,
            chosen != null && own == null,
            parent != null ? parent.getName() : null,
            chosen != null ? chosen.getLabel() : null);
    }

    private List<AccommodationEmail> active(Accommodation property) {
        List<AccommodationEmail> list = new ArrayList<>();
        if (property.getEmails() == null) return list;
        for (AccommodationEmail email : property.getEmails()) {
            if (email.getEmail() != null && !Boolean.FALSE.equals(email.getIsActive())) list.add(email);
        }
        /* sorted before anything is chosen, so a group with reservations1/2/3@ picks predictably */
        list.sort((a, b) -> a.getEmail().compareToIgnoreCase(b.getEmail()));
        return list;
    }

    /**
     * The best address for this purpose.
     *
     * A preferred type that is also marked primary wins outright; then any address of a preferred
     * type; then whatever is primary, because somebody chose it; then the general inbox. Falling
     * through to the first is better than falling through to nothing.
     */
    private AccommodationEmail best(
        List<AccommodationEmail> list,
        List<AccommodationEmail.EmailType> preferred
    ) {
        for (AccommodationEmail.EmailType type : preferred) {
            for (AccommodationEmail email : list) {
                if (email.getEmailType() == type && Boolean.TRUE.equals(email.getIsPrimary())) return email;
            }
        }
        for (AccommodationEmail.EmailType type : preferred) {
            for (AccommodationEmail email : list) {
                if (email.getEmailType() == type) return email;
            }
        }
        for (AccommodationEmail email : list) {
            if (Boolean.TRUE.equals(email.getIsPrimary())) return email;
        }
        for (AccommodationEmail email : list) {
            if (email.getEmailType() == AccommodationEmail.EmailType.GENERAL) return email;
        }
        return list.isEmpty() ? null : list.get(0);
    }
}

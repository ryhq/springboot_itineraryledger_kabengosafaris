package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;

import lombok.Data;

/**
 * Everything a caller can narrow the email-account list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card cannot
 * report a figure the table would contradict, and prev/next cannot wander out of the set on
 * screen. Spring binds it with {@code @ModelAttribute}, so every parameter the old signature
 * took is still spelled the same on the wire.
 */
@Data
public class EmailAccountFilter {

    /** Free text across the address, the name, the description and the host. */
    private String keyword;

    private String email;
    private String name;
    private String description;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String errorMessage;

    private EmailAccountProvider providerType;
    private List<EmailAccountProvider> providerTypes;

    private Boolean enabled;
    /** "enabled" / "disabled"; a contradictory pair cancels to no constraint. */
    private List<String> statuses;

    private Boolean isDefault;
    private Boolean hasErrors;
    private Boolean useTls;
    private Boolean useSsl;

    /**
     * Worth checking, each of which is also a card.
     *
     * Sending mail is the one thing this system does that a customer sees. An account whose
     * last send failed, or that nobody has ever tested, is a silent outage waiting for the
     * next invoice; one that receives but has not fetched in a while is a mailbox filling up
     * unread.
     */
    private List<String> qualities;

    private LocalDateTime createdAfter;

    public List<EmailAccountProvider> allProviderTypes() {
        List<EmailAccountProvider> out = new ArrayList<>();
        if (providerTypes != null) providerTypes.stream().filter(Objects::nonNull).forEach(out::add);
        if (providerType != null && !out.contains(providerType)) out.add(providerType);
        return out;
    }

    public Boolean resolvedEnabled() {
        boolean yes = statuses != null && statuses.contains("enabled");
        boolean no = statuses != null && statuses.contains("disabled");
        if (yes ^ no) return yes;
        return enabled;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }
}

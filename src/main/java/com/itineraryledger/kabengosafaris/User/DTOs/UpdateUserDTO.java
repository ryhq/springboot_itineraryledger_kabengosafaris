package com.itineraryledger.kabengosafaris.User.DTOs;

import java.util.List;

import lombok.Data;

/**
 * An administrator's edit of somebody else's account.
 *
 * Null means "leave it", which is what lets the record page save one field at a time
 * without the other fields being clobbered by whatever the form last loaded.
 *
 * There is no password field on purpose. An administrator resetting a colleague's
 * password to a value they know is a way to sign in as them; the reset endpoint mails
 * a link instead, so only the person holding the mailbox can complete it.
 */
@Data
public class UpdateUserDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String phoneNumber;
    private String bio;

    private Boolean enabled;

    /**
     * Replaces the whole set when sent, so an empty list genuinely means "no roles"
     * rather than "no change" — which is why it is only read when non-null.
     */
    private List<String> roleIds;
}

package com.itineraryledger.kabengosafaris.User.DTOs;

import java.util.List;

import lombok.Data;

/**
 * What an administrator supplies to give somebody an account.
 *
 * Not the same shape as RegistrationRequest, which is self-signup: here nobody is
 * choosing their own password, so `password` is optional and a strong one is
 * generated when it is left out. The account is created disabled either way and an
 * activation link is emailed, so the person sets their own password on arrival and
 * no temporary one ever has to be read out over the phone.
 */
@Data
public class CreateUserDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String phoneNumber;

    /**
     * Optional. Left blank, a strong one is generated and never shown to anybody —
     * the activation email is what lets the account in.
     */
    private String password;

    /**
     * The roles to grant on creation, by obfuscated id.
     *
     * Worth sending: an account with no roles can sign in and do nothing, which
     * looks like a broken system rather than an unfinished setup.
     */
    private List<String> roleIds;

    /**
     * A single role, for callers that only ever grant one.
     *
     * The create form offers one picker, and adding an account from inside a role's own
     * page grants that role — both send one id, and coercing a lone string into a list
     * is not something Jackson does by default. Merged with roleIds by allRoleIds().
     */
    private String roleId;

    /** Default true. False creates the account without emailing anybody. */
    private Boolean sendInvite;

    /** Both role fields, de-duplicated. */
    public List<String> allRoleIds() {
        List<String> out = new java.util.ArrayList<>();
        if (roleIds != null) roleIds.stream().filter(r -> r != null && !r.isBlank()).forEach(out::add);
        if (roleId != null && !roleId.isBlank() && !out.contains(roleId)) out.add(roleId);
        return out;
    }
}

package com.itineraryledger.kabengosafaris.User.Controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.BulkFlags;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.CreateUserDTO;
import com.itineraryledger.kabengosafaris.User.DTOs.UpdateUserDTO;
import com.itineraryledger.kabengosafaris.User.Services.UserAdminServices.UserAdminCreateService;
import com.itineraryledger.kabengosafaris.User.Services.UserAdminServices.UserAdminDeleteService;
import com.itineraryledger.kabengosafaris.User.Services.UserAdminServices.UserAdminGetService;
import com.itineraryledger.kabengosafaris.User.Services.UserAdminServices.UserAdminUpdateService;
import com.itineraryledger.kabengosafaris.User.Specifications.UserFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Managing the people who can log in.
 *
 * Separate from UserController, which is `/api/user/me` and is about oneself. This is
 * about everybody else, and every endpoint is permissioned as USER rather than as its
 * own thing, so a role granting "users" grants exactly this screen.
 *
 * The write endpoints take the caller's own account, because several changes have to
 * be refused when the target is the caller — deactivating yourself, locking yourself
 * out, dropping your own administration rights. See UserAdminUpdateService.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserAdminController {

    private final UserAdminGetService getService;
    private final UserAdminCreateService createService;
    private final UserAdminUpdateService updateService;
    private final UserAdminDeleteService deleteService;
    private final UserRepository userRepository;
    private final BulkFlags bulkFlags;

    /**
     * The list, its counters and its sort — one endpoint.
     *
     * The filter arrives as a @ModelAttribute so every dimension is a query param and
     * the whole state stays in the URL, which is what makes a filtered list shareable
     * and the back button behave.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_USER')")
    public ResponseEntity<ApiResponse<?>> list(
        @ModelAttribute UserFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return getService.list(filter, includeStats, page, size, sortBy, sortDirection);
    }

    /**
     * One account, plus where it sits in the caller's filtered set.
     *
     * The filter comes along so prev/next walks the same list the user was looking at,
     * rather than every account in id order.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_USER')")
    public ResponseEntity<ApiResponse<?>> getOne(
        @PathVariable String id,
        @ModelAttribute UserFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return getService.getOne(id, filter, sortBy, sortDirection);
    }

    /** Whether this account can be deleted, and what is stopping it. */
    @GetMapping("/{id}/deletable")
    @PreAuthorize("hasAuthority('PERM_DELETE_USER')")
    public ResponseEntity<ApiResponse<?>> deletable(@PathVariable String id) {
        return deleteService.checkDeletable(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_USER')")
    public ResponseEntity<ApiResponse<?>> create(@RequestBody CreateUserDTO request) {
        return createService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<ApiResponse<?>> update(
        @PathVariable String id,
        @RequestBody UpdateUserDTO request,
        Authentication authentication
    ) {
        return updateService.update(id, request, actor(authentication));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<ApiResponse<?>> deactivate(
        @PathVariable String id, Authentication authentication) {
        return updateService.setEnabled(id, false, actor(authentication));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<ApiResponse<?>> reactivate(
        @PathVariable String id, Authentication authentication) {
        return updateService.setEnabled(id, true, actor(authentication));
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<ApiResponse<?>> lock(
        @PathVariable String id, Authentication authentication) {
        return updateService.setLocked(id, true, actor(authentication));
    }

    /** Also clears the failed-attempt counter, or the next wrong password re-locks it. */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<ApiResponse<?>> unlock(
        @PathVariable String id, Authentication authentication) {
        return updateService.setLocked(id, false, actor(authentication));
    }

    /** For the lost phone: clears the enrolment so they can set it up again. */
    @PostMapping("/{id}/reset-mfa")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<ApiResponse<?>> resetMfa(@PathVariable String id) {
        return updateService.resetMfa(id);
    }

    /** Mails them a reset link. Never sets a password an administrator would know. */
    @PostMapping("/{id}/send-password-reset")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<ApiResponse<?>> sendPasswordReset(@PathVariable String id) {
        return updateService.sendPasswordReset(id);
    }

    @PostMapping("/{id}/resend-invite")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<ApiResponse<?>> resendInvite(@PathVariable String id) {
        return updateService.resendInvite(id);
    }

    /**
     * Activating or deactivating a selection in one request.
     *
     * Fifty separate calls can each fail on their own and leave the set half-changed
     * with nobody the wiser; this reports per-id outcomes.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_USER')")
    public ResponseEntity<?> bulkUpdate(
        @RequestBody BulkFlags.Request request,
        Authentication authentication
    ) {
        User actor = actor(authentication);
        return bulkFlags.apply("account", userRepository, request, user -> {
            if (request.getIsActive() != null) {
                /*
                 * The self-lockout guard has to hold here too, and a bulk call is the
                 * likeliest place to trip it: select all, deactivate, and your own row
                 * was in the selection.
                 */
                if (Boolean.FALSE.equals(request.getIsActive())
                    && actor != null && actor.getId().equals(user.getId())) {
                    throw new IllegalStateException("You cannot deactivate your own account");
                }
                user.setEnabled(request.getIsActive());
            }
        });
    }

    /** Reference-checked; refuses rather than emptying an audit stamp. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_USER')")
    public ResponseEntity<ApiResponse<?>> delete(
        @PathVariable String id, Authentication authentication) {
        return deleteService.delete(List.of(id), actor(authentication));
    }

    /** Bare array body, as everywhere else in this API. */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_USER')")
    public ResponseEntity<ApiResponse<?>> deleteBatch(
        @RequestBody List<String> idList, Authentication authentication) {
        return deleteService.delete(idList, actor(authentication));
    }

    /** The caller's own account, or null when the principal is not one. */
    private User actor(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) return null;
        return user;
    }
}

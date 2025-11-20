package com.itineraryledger.kabengosafaris.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Find all users with locked accounts.
     * Used by scheduled task to automatically unlock expired accounts.
     */
    List<User> findByAccountLockedTrue();

    /**
     * Find all users with failed login attempts greater than specified count.
     * Used by scheduled task to reset failed attempt counters.
     */
    List<User> findByFailedAttemptGreaterThan(int failedAttempt);

}

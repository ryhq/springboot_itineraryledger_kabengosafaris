package com.itineraryledger.kabengosafaris.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

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

    /**
     * Count enabled (active) users
     */
    long countByEnabledTrue();

    /**
     * How many users hold each of the given roles, in one query.
     *
     * The role list needs a holder count per row, and asking per row is a query per
     * role — the N+1 that makes a list page feel slow for no reason. Roles nobody holds
     * are simply absent from the result, so the caller defaults them to zero.
     *
     * @return rows of [roleId, count]
     */
    @Query("select r.id, count(u.id) from User u join u.roles r where r.id in :roleIds group by r.id")
    List<Object[]> countUsersByRoleIds(@Param("roleIds") Collection<Long> roleIds);

}

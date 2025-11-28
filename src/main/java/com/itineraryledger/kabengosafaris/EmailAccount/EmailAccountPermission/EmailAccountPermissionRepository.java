package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountPermission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for EmailAccountPermission entity
 * Manages access control to email account for users and roles
 */
@Repository
public interface EmailAccountPermissionRepository extends  JpaRepository<EmailAccountPermission, Long>{
    
}

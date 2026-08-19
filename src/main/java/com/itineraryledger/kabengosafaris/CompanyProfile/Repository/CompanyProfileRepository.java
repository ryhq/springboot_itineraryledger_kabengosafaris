package com.itineraryledger.kabengosafaris.CompanyProfile.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyProfile;

@Repository
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {

    /**
     * The one row, with everything a document needs already loaded.
     *
     * Rendering a single letter reads the name, the primary email, the primary phone, the address and
     * a logo — five lazy collections, which without this is five queries per letter and a
     * LazyInitializationException the first time one is rendered outside a transaction.
     */
    @Query("""
        select distinct p from CompanyProfile p
        left join fetch p.emails
        left join fetch p.phones
        left join fetch p.addresses
        left join fetch p.links
        left join fetch p.assets
        order by p.id asc
        """)
    java.util.List<CompanyProfile> findAllWithDetails();

    default Optional<CompanyProfile> findSingleton() {
        return findAllWithDetails().stream().findFirst();
    }
}

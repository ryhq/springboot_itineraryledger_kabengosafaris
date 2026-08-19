package com.itineraryledger.kabengosafaris.CompanyProfile.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyProfile;

@Repository
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {

    /**
     * The one row.
     *
     * Deliberately NOT one query that join-fetches all five collections: Hibernate refuses to fetch
     * two Lists at once (MultipleBagFetchException), which is exactly how the first version of this
     * failed — the endpoints answered 500 while the health check stayed green, because nothing had
     * asked for the profile until a real request did.
     *
     * The collections are @BatchSize-ed instead, so a caller inside a transaction pays one small
     * query per collection rather than one per row, and callers outside one must not touch them.
     */
    default Optional<CompanyProfile> findSingleton() {
        List<CompanyProfile> rows = findAll(Sort.by(Sort.Direction.ASC, "id"));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}

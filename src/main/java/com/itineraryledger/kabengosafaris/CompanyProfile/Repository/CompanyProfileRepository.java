package com.itineraryledger.kabengosafaris.CompanyProfile.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Stamp the outcome of a website cache call onto the profile.
     *
     * <p>Three columns by id, not {@code save(profile)}. The caller runs after its own transaction
     * has committed — often on another thread with no transaction at all — so the profile it holds
     * is detached and its five collections are uninitialised proxies. Merging that entity back
     * would have Hibernate cascade into collections it cannot load, which is a
     * LazyInitializationException raised by a bookkeeping write that nobody was waiting for.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE CompanyProfile c
           SET c.websiteCacheLastCalledAt = :at,
               c.websiteCacheLastOk = :ok,
               c.websiteCacheLastDetail = :detail
         WHERE c.id = :id
        """)
    int recordCacheCall(@Param("id") Long id,
                        @Param("at") java.time.LocalDateTime at,
                        @Param("ok") Boolean ok,
                        @Param("detail") String detail);
}

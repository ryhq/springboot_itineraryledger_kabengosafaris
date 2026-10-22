package com.itineraryledger.kabengosafaris.Legal.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocument;
import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocumentType;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {

    Optional<LegalDocument> findByDocumentTypeAndPublishedTrue(LegalDocumentType documentType);

    Optional<LegalDocument> findByDocumentTypeAndVersion(LegalDocumentType documentType, String version);

    List<LegalDocument> findByDocumentTypeOrderByEffectiveFromDesc(LegalDocumentType documentType);

    /** Publishing one unpublishes the others, so a site can never be asked which of two is live. */
    @Modifying
    @Query("update LegalDocument d set d.published = false"
        + " where d.documentType = :type and d.id <> :keep")
    int unpublishOthers(@Param("type") LegalDocumentType type, @Param("keep") Long keep);
}

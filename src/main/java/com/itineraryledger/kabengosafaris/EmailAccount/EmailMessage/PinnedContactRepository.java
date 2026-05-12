package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.PinnedContact;

@Repository
public interface PinnedContactRepository extends JpaRepository<PinnedContact, Long> {
    List<PinnedContact> findByEmailAccountIdOrderByCreatedAtAsc(Long emailAccountId);
    Optional<PinnedContact> findByEmailAccountIdAndEmail(Long emailAccountId, String email);
}

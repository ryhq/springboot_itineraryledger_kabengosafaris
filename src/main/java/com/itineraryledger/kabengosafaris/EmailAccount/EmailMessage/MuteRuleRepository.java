package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.MuteRule;

@Repository
public interface MuteRuleRepository extends JpaRepository<MuteRule, Long> {
    List<MuteRule> findByEmailAccountIdOrderByCreatedAtAsc(Long emailAccountId);
    List<MuteRule> findByEmailAccountIdAndIsActiveTrue(Long emailAccountId);
}

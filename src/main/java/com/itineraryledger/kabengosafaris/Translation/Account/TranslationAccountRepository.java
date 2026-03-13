package com.itineraryledger.kabengosafaris.Translation.Account;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationAccount;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;

@Repository
public interface TranslationAccountRepository extends JpaRepository<TranslationAccount, Long>, JpaSpecificationExecutor<TranslationAccount> {

    Optional<TranslationAccount> findByName(String name);

    boolean existsByProviderType(TranslationProviderType providerType);

    @Modifying
    @Query("UPDATE TranslationAccount SET isDefault = false WHERE id != :id")
    void setOnlyOneDefault(@Param("id") Long id);

    Optional<TranslationAccount> findFirstByEnabledTrueAndIsDefaultTrueOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE TranslationAccount t SET t.charactersTranslated = t.charactersTranslated + :chars, t.requestsMade = t.requestsMade + 1 WHERE t.id = :id")
    void incrementStats(@Param("id") Long id, @Param("chars") long chars);

    @Modifying
    @Query("UPDATE TranslationAccount t SET t.requestsFailed = t.requestsFailed + 1, t.requestsMade = t.requestsMade + 1 WHERE t.id = :id")
    void incrementFailures(@Param("id") Long id);

    @Query("SELECT t.id FROM TranslationAccount t WHERE t.id > :currentId ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT t.id FROM TranslationAccount t WHERE t.id < :currentId ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT t.id FROM TranslationAccount t ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT t.id FROM TranslationAccount t ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findLastId();
}

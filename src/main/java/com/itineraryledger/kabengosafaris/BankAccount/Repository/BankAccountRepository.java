package com.itineraryledger.kabengosafaris.BankAccount.Repository;

import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long>, JpaSpecificationExecutor<BankAccount> {

    Optional<BankAccount> findByAccountCode(String accountCode);

    List<BankAccount> findByCurrency(String currency);

    List<BankAccount> findByCurrencyAndIsActive(String currency, Boolean isActive);

    Optional<BankAccount> findByCurrencyAndIsDefaultAndIsActive(String currency, Boolean isDefault, Boolean isActive);

    List<BankAccount> findByIsActiveOrderByAccountNameAsc(Boolean isActive);

    List<BankAccount> findByCurrencyInAndIsActive(List<String> currencies, Boolean isActive);

    boolean existsByAccountCode(String accountCode);

    List<BankAccount> findByCurrencyAndIsDefault(String currency, Boolean isDefault);

    @Query("SELECT e.id FROM BankAccount e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM BankAccount e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM BankAccount e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM BankAccount e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}

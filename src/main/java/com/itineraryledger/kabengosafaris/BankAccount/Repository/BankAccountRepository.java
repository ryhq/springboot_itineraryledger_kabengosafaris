package com.itineraryledger.kabengosafaris.BankAccount.Repository;

import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}

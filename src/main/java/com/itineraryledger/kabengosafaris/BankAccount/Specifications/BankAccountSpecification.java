package com.itineraryledger.kabengosafaris.BankAccount.Specifications;

import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import org.springframework.data.jpa.domain.Specification;

public class BankAccountSpecification {

    public static Specification<BankAccount> byCurrency(String currency) {
        return (root, query, criteriaBuilder) -> {
            if (currency == null || currency.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("currency"), currency.trim().toUpperCase());
        };
    }

    public static Specification<BankAccount> byIsActive(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<BankAccount> byIsDefault(Boolean isDefault) {
        return (root, query, criteriaBuilder) -> {
            if (isDefault == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("isDefault"), isDefault);
        };
    }

    public static Specification<BankAccount> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("accountName")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("accountCode")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("bankName")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("bankBranch")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("branchCity")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("branchCountry")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("accountNumber")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("accountHolderName")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("swiftBicCode")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("iban")), likePattern)
            );
        };
    }

    public static Specification<BankAccount> byCurrencies(java.util.List<String> currencies) {
        return (root, query, cb) -> currencies == null || currencies.isEmpty()
            ? cb.conjunction()
            : cb.upper(root.get("currency")).in(currencies.stream().map(String::toUpperCase).toList());
    }

    /* What a transfer needs, and what stops one arriving. */

    /** No SWIFT/BIC: an international transfer cannot be addressed to it. */
    public static Specification<BankAccount> missingSwift() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("swiftBicCode")),
            cb.equal(cb.trim(root.get("swiftBicCode")), ""));
    }

    /** No IBAN: a European transfer cannot be addressed to it. */
    public static Specification<BankAccount> missingIban() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("iban")),
            cb.equal(cb.trim(root.get("iban")), ""));
    }
}

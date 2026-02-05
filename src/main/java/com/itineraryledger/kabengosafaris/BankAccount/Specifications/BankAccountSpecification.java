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
}

package com.itineraryledger.kabengosafaris.Translation.Account.Services;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationAccount;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;

public class TranslationAccountSpecification {

    public static Specification<TranslationAccount> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<TranslationAccount> descriptionLike(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
        };
    }

    public static Specification<TranslationAccount> providerType(TranslationProviderType providerType) {
        return (root, query, cb) -> {
            if (providerType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("providerType"), providerType);
        };
    }

    public static Specification<TranslationAccount> isEnabled(Boolean enabled) {
        return (root, query, cb) -> {
            if (enabled == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("enabled"), enabled);
        };
    }

    public static Specification<TranslationAccount> isDefault(Boolean isDefault) {
        return (root, query, cb) -> {
            if (isDefault == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isDefault"), isDefault);
        };
    }

    public static Specification<TranslationAccount> hasErrors() {
        return (root, query, cb) -> cb.isNotNull(root.get("lastErrorMessage"));
    }

    public static Specification<TranslationAccount> baseUrlLike(String baseUrl) {
        return (root, query, cb) -> {
            if (baseUrl == null || baseUrl.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("baseUrl")), "%" + baseUrl.toLowerCase() + "%");
        };
    }
}

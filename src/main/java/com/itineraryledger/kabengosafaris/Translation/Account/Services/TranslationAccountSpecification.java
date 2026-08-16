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

    // ========================
    // MULTI-VALUE FACETS + WORTH CHECKING
    // ========================

    /** Any of these providers. An empty list is no constraint, never "nothing". */
    public static Specification<TranslationAccount> providerTypeIn(
        java.util.List<TranslationProviderType> types
    ) {
        return (root, query, cb) -> types == null || types.isEmpty()
            ? cb.conjunction()
            : root.get("providerType").in(types);
    }

    /**
     * The one search box: what the account is called, what it is for, and where it points.
     *
     * Never the API key. It is not returned by any endpoint and must not become searchable
     * by accident — "does anybody hold this key" is not a question this list should answer.
     */
    public static Specification<TranslationAccount> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("baseUrl")), like));
        };
    }

    /**
     * Never tested — nobody has ever proved this account works.
     *
     * A translation provider fails silently: the text simply comes back in English. So an
     * untested account is the same failure as a broken one, just not discovered yet.
     */
    public static Specification<TranslationAccount> neverTested() {
        return (root, query, cb) -> cb.isNull(root.get("lastTestedAt"));
    }

    public static Specification<TranslationAccount> createdAfter(java.time.LocalDateTime when) {
        return (root, query, cb) -> when == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), when);
    }
}

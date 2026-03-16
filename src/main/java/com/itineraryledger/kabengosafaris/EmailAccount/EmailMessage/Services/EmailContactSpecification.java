package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact.ContactSource;

public class EmailContactSpecification {

    public static Specification<EmailContact> forAccount(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("emailAccount").get("id"), accountId);
    }

    public static Specification<EmailContact> isStarred(Boolean starred) {
        if (starred == null) return null;
        return (root, query, cb) -> cb.equal(root.get("isStarred"), starred);
    }

    public static Specification<EmailContact> hasSource(String source) {
        if (source == null || source.isBlank()) return null;
        try {
            ContactSource contactSource = ContactSource.valueOf(source.toUpperCase());
            return (root, query, cb) -> cb.equal(root.get("source"), contactSource);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Specification<EmailContact> searchTerm(String search) {
        if (search == null || search.isBlank()) return null;
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("emailAddress")), pattern),
            cb.like(cb.lower(root.get("displayName")), pattern)
        );
    }
}

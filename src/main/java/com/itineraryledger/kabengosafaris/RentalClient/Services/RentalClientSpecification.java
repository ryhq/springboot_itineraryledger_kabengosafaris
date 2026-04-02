package com.itineraryledger.kabengosafaris.RentalClient.Services;

import com.itineraryledger.kabengosafaris.RentalClient.Entity.RentalClient;
import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import org.springframework.data.jpa.domain.Specification;

public class RentalClientSpecification {

    public static Specification<RentalClient> hasClientType(RentalClientType clientType) {
        return (root, query, cb) -> {
            if (clientType == null) return cb.conjunction();
            return cb.equal(root.get("clientType"), clientType);
        };
    }

    public static Specification<RentalClient> firstNameLike(String firstName) {
        return (root, query, cb) -> {
            if (firstName == null || firstName.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%");
        };
    }

    public static Specification<RentalClient> lastNameLike(String lastName) {
        return (root, query, cb) -> {
            if (lastName == null || lastName.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%");
        };
    }

    public static Specification<RentalClient> companyNameLike(String companyName) {
        return (root, query, cb) -> {
            if (companyName == null || companyName.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("companyName")), "%" + companyName.toLowerCase() + "%");
        };
    }

    public static Specification<RentalClient> phoneLike(String phone) {
        return (root, query, cb) -> {
            if (phone == null || phone.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("phone")), "%" + phone.toLowerCase() + "%");
        };
    }

    public static Specification<RentalClient> emailLike(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }

    public static Specification<RentalClient> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return cb.conjunction();
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<RentalClient> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(root.get("companyName")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }
}

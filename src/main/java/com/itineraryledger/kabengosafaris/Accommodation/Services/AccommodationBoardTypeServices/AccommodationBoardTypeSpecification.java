package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationBoardTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import org.springframework.data.jpa.domain.Specification;

/**
 * AccommodationBoardTypeSpecification - Specification for filtering accommodation board types
 */
public class AccommodationBoardTypeSpecification {

    /**
     * Filter by accommodation ID
     */
    public static Specification<AccommodationBoardType> hasAccommodationId(Long accommodationId) {
        return (root, query, cb) -> cb.equal(root.get("accommodation").get("id"), accommodationId);
    }

    /**
     * Filter by name (partial match, case-insensitive)
     */
    public static Specification<AccommodationBoardType> hasName(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Filter by exact name
     */
    public static Specification<AccommodationBoardType> hasExactName(String name) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("name")), name.toLowerCase());
    }

    /**
     * Filter by breakfast included
     */
    public static Specification<AccommodationBoardType> hasBreakfastIncluded(Boolean breakfastIncluded) {
        return (root, query, cb) -> cb.equal(root.get("breakfastIncluded"), breakfastIncluded);
    }

    /**
     * Filter by lunch included
     */
    public static Specification<AccommodationBoardType> hasLunchIncluded(Boolean lunchIncluded) {
        return (root, query, cb) -> cb.equal(root.get("lunchIncluded"), lunchIncluded);
    }

    /**
     * Filter by dinner included
     */
    public static Specification<AccommodationBoardType> hasDinnerIncluded(Boolean dinnerIncluded) {
        return (root, query, cb) -> cb.equal(root.get("dinnerIncluded"), dinnerIncluded);
    }

    /**
     * Filter by drinks included
     */
    public static Specification<AccommodationBoardType> hasDrinksIncluded(Boolean drinksIncluded) {
        return (root, query, cb) -> cb.equal(root.get("drinksIncluded"), drinksIncluded);
    }

    /**
     * Filter by alcoholic drinks included
     */
    public static Specification<AccommodationBoardType> hasAlcoholicDrinksIncluded(Boolean alcoholicDrinksIncluded) {
        return (root, query, cb) -> cb.equal(root.get("alcoholicDrinksIncluded"), alcoholicDrinksIncluded);
    }

    /**
     * Filter by active status
     */
    public static Specification<AccommodationBoardType> isActive(Boolean isActive) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }

    /**
     * Filter board types that have full meal plan (breakfast + lunch + dinner)
     */
    public static Specification<AccommodationBoardType> hasFullMealPlan() {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("breakfastIncluded"), true),
            cb.equal(root.get("lunchIncluded"), true),
            cb.equal(root.get("dinnerIncluded"), true)
        );
    }

    /**
     * Search by keyword across name, description, and meal details
     */
    public static Specification<AccommodationBoardType> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("mealsIncluded")), pattern),
                cb.like(cb.lower(root.get("inclusions").as(String.class)), pattern)
            );
        };
    }
}

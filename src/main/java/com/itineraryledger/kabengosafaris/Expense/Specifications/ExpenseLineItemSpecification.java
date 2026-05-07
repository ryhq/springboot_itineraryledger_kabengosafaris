package com.itineraryledger.kabengosafaris.Expense.Specifications;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseLineItem;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseCategory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class ExpenseLineItemSpecification {

    private ExpenseLineItemSpecification() {}

    public static Specification<ExpenseLineItem> byExpenseId(Long expenseId) {
        return (root, query, cb) -> expenseId == null ? cb.conjunction() : cb.equal(root.get("expense").get("id"), expenseId);
    }

    public static Specification<ExpenseLineItem> byCategory(ExpenseCategory category) {
        return (root, query, cb) -> category == null ? cb.conjunction() : cb.equal(root.get("category"), category);
    }

    public static Specification<ExpenseLineItem> byItemName(String itemName) {
        return (root, query, cb) -> {
            if (itemName == null || itemName.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("itemName")), "%" + itemName.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ExpenseLineItem> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null ? cb.conjunction() : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<ExpenseLineItem> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            Predicate name = cb.like(cb.lower(root.get("itemName")), like);
            Predicate desc = cb.like(cb.lower(root.get("description")), like);
            return cb.or(name, desc);
        };
    }
}

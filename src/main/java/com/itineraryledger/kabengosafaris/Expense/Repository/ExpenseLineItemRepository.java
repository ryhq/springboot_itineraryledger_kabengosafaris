package com.itineraryledger.kabengosafaris.Expense.Repository;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseLineItemRepository extends JpaRepository<ExpenseLineItem, Long>, JpaSpecificationExecutor<ExpenseLineItem> {

    List<ExpenseLineItem> findByExpenseIdOrderByDisplayOrderAsc(Long expenseId);
    List<ExpenseLineItem> findByExpenseIdAndIsActiveTrueOrderByDisplayOrderAsc(Long expenseId);

    @Query("SELECT COALESCE(MAX(i.displayOrder), -1) FROM ExpenseLineItem i WHERE i.expense.id = :expenseId")
    Integer findMaxDisplayOrderByExpenseId(@Param("expenseId") Long expenseId);

    long countByExpenseId(Long expenseId);
    void deleteByExpenseId(Long expenseId);

    // Parent-scoped navigation (avoids the cross-parent leak we hit on Invoice line items)
    @Query("SELECT e.id FROM ExpenseLineItem e WHERE e.expense.id = :parentId AND e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextIdInExpense(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT e.id FROM ExpenseLineItem e WHERE e.expense.id = :parentId AND e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInExpense(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT e.id FROM ExpenseLineItem e WHERE e.expense.id = :parentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstIdInExpense(@Param("parentId") Long parentId);

    @Query("SELECT e.id FROM ExpenseLineItem e WHERE e.expense.id = :parentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastIdInExpense(@Param("parentId") Long parentId);
}

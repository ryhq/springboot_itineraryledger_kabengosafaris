package com.itineraryledger.kabengosafaris.Expense.Repository;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseDocumentRepository extends JpaRepository<ExpenseDocument, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<ExpenseDocument> {

    List<ExpenseDocument> findByExpenseIdOrderByCreatedAtDesc(Long expenseId);
    List<ExpenseDocument> findByExpensePaymentIdOrderByCreatedAtDesc(Long expensePaymentId);
    long countByExpenseId(Long expenseId);

    Optional<ExpenseDocument> findByFileName(String fileName);

    @Query("SELECT d FROM ExpenseDocument d WHERE d.expense.id = :expenseId AND d.isActive = true ORDER BY d.createdAt DESC")
    List<ExpenseDocument> findActiveByExpenseId(@Param("expenseId") Long expenseId);
}

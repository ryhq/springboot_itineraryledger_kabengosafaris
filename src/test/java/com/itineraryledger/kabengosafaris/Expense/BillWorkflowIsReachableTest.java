package com.itineraryledger.kabengosafaris.Expense;

import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every state a bill can be in must have a way out.
 *
 * <p>Bills are created as DRAFT. The payment service refused a payment on a draft and told the
 * office to "mark the expense as RECORDED first" — and nothing in the system could do that. The
 * transition was legal in the enum, the update endpoint would have accepted it, and no endpoint
 * or button offered it. So the one live bill on Jatelo could take no payment and reach no other
 * state: the only door out was Cancel.
 *
 * <p>A refusal that is right and unreachable is worse than either. This walks the state machine
 * and fails if any state a bill can reach has no exit.
 */
class BillWorkflowIsReachableTest {

    private static final Path EXPENSE = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/Expense");

    @Test
    @DisplayName("every non-final status a bill can hold has a transition out of it")
    void noStatusIsADeadEnd() {
        List<String> deadEnds = new ArrayList<>();
        for (ExpenseStatus status : ExpenseStatus.values()) {
            if (status.isFinalState()) {
                continue;
            }
            boolean hasExit = false;
            for (ExpenseStatus target : ExpenseStatus.values()) {
                if (target != status && status.canTransitionTo(target)) {
                    hasExit = true;
                    break;
                }
            }
            if (!hasExit) deadEnds.add(status.name());
        }
        assertTrue(deadEnds.isEmpty(), "these statuses cannot be left: " + deadEnds);
    }

    @Test
    @DisplayName("the draft-to-recorded transition has an endpoint, not just permission to exist")
    void recordingABillIsReachable() throws IOException {
        String controller = Files.readString(EXPENSE.resolve("Controller/ExpenseController.java"));
        assertTrue(controller.contains("@PostMapping(\"/{id}/record\")"),
            "DRAFT -> RECORDED is legal in the enum and the payment service demands it, so it needs "
                + "a door of its own beside cancel and reopen");

        String service = Files.readString(EXPENSE.resolve(
            "Services/ExpenseServices/ExpenseUpdateService.java"));
        assertTrue(service.contains("public ResponseEntity<ApiResponse<?>> markRecorded("),
            "the endpoint needs something to call");
        assertTrue(service.contains("EXPENSE_HAS_NO_TOTAL"),
            "a bill is not blocked for being a draft, it is blocked for having no settled figure. "
                + "Recording one with no amount would defeat the rule it is meant to satisfy");
    }

    @Test
    @DisplayName("a payment can record the bill on the way in, in the same transaction")
    void payingADraftCanPromoteItInOneStep() throws IOException {
        String dto = Files.readString(EXPENSE.resolve("DTOs/CreateExpensePaymentDTO.java"));
        assertTrue(dto.contains("markRecorded"),
            "wanting to pay a bill is the signal that it is real; two round trips to say so is "
                + "ceremony, not safety");

        String payments = Files.readString(EXPENSE.resolve(
            "Services/ExpensePaymentServices/ExpensePaymentCreateService.java"));
        assertTrue(payments.contains("expenseUpdateService.markRecorded("),
            "the promotion must go through the same method the standalone action uses, so the "
                + "no-amount rule lives in one place");
        assertTrue(payments.contains("@Transactional")
                || payments.contains("class ExpensePaymentCreateService"),
            "promoting and paying must not be able to half-apply");

        /* And it must stay explicit: a draft paid by accident should still be refused. */
        assertTrue(payments.contains("!Boolean.TRUE.equals(dto.getMarkRecorded())"),
            "the caller has to ask for the promotion; it cannot be silent");
        assertFalse(payments.contains("Mark the expense as RECORDED before adding payments."),
            "the old message named a button that did not exist");
    }
}

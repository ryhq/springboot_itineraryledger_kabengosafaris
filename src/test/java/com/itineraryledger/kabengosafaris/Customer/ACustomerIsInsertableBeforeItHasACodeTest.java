package com.itineraryledger.kabengosafaris.Customer;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A customer has to survive the statement that gives it an id.
 *
 * <p>{@code code} is NOT NULL and {@code @NotBlank}, and it is derived from the id, which only an
 * insert can supply. So every path that creates a customer writes the row once with a stand-in
 * code and then codes it properly. Converting an enquirer skipped the stand-in, and Hibernate
 * refused the insert with "Customer code is required" -- reported to the office as "An unexpected
 * error occurred", because that method committed outside its own try block.
 *
 * <p>This test asks the real validator the question Hibernate asks at persist time, so the answer
 * cannot drift from what the database will actually accept.
 */
class ACustomerIsInsertableBeforeItHasACodeTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    @Test
    @DisplayName("a customer built with a provisional code passes the checks the insert runs")
    void aProvisionalCodeIsEnoughToBeWritten() {
        Customer customer = enquirerTurnedCustomer(Customer.provisionalCode());

        Set<ConstraintViolation<Customer>> violations = VALIDATOR.validate(customer);

        assertTrue(violations.isEmpty(),
            "these are the same constraints Hibernate applies on pre-insert: " + describe(violations));
    }

    @Test
    @DisplayName("and without one the insert is refused, naming the field")
    void withoutACodeTheInsertIsRefused() {
        Customer customer = enquirerTurnedCustomer(null);

        Set<ConstraintViolation<Customer>> violations = VALIDATOR.validate(customer);

        assertEquals(1, violations.size(), describe(violations));
        ConstraintViolation<Customer> only = violations.iterator().next();
        assertEquals("code", only.getPropertyPath().toString(),
            "this is exactly what stopped every attempt to convert an enquiry");
    }

    @Test
    @DisplayName("two provisional codes are never the same")
    void provisionalCodesDoNotCollide() {
        /*
         * The literal "TEMP" was used for this, and code carries a unique constraint. Two people
         * creating a customer in the same moment collided on it, and the loser's whole
         * transaction failed for a reason that had nothing to do with either customer.
         */
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) seen.add(Customer.provisionalCode());

        assertEquals(500, seen.size(), "a stand-in on a unique column must be unique");
        assertNotEquals("TEMP", Customer.provisionalCode());
    }

    @Test
    @DisplayName("the provisional code fits the column and is obviously not a real one")
    void provisionalCodesAreRecognisable() {
        String code = Customer.provisionalCode();

        assertTrue(code.length() <= 50, "the column is 50 characters: " + code);
        assertTrue(code.startsWith("NEW-"),
            "somebody reading a customer list needs to see at a glance that this one never got "
            + "its real code, rather than wondering what CUS- prefix they are looking at");
    }

    @Test
    @DisplayName("the real code comes from the id, so it cannot collide either")
    void theRealCodeFollowsTheId() {
        Customer first = enquirerTurnedCustomer(Customer.provisionalCode());
        Customer second = enquirerTurnedCustomer(Customer.provisionalCode());
        org.springframework.test.util.ReflectionTestUtils.setField(first, "id", 11L);
        org.springframework.test.util.ReflectionTestUtils.setField(second, "id", 12L);

        assertEquals("CUS-000111", first.generateCode());
        assertEquals("CUS-000112", second.generateCode());
    }

    private Customer enquirerTurnedCustomer(String code) {
        return Customer.builder()
            .code(code)
            .customerType(CustomerType.INDIVIDUAL)
            .firstName("Rukaiya")
            .lastName("Docrat")
            .nationality("South Africa")
            .country("South Africa")
            .preferredLanguage("en")
            .preferredCurrency("USD")
            .isActive(true)
            .isVip(false)
            .build();
    }

    private String describe(Set<ConstraintViolation<Customer>> violations) {
        StringBuilder out = new StringBuilder();
        for (ConstraintViolation<Customer> v : violations) {
            out.append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("; ");
        }
        return out.toString();
    }
}

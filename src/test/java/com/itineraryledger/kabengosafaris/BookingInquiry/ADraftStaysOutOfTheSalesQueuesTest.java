package com.itineraryledger.kabengosafaris.BookingInquiry;

import com.itineraryledger.kabengosafaris.BookingInquiry.Specifications.BookingInquirySpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A lead that is still being typed is not work anybody can do yet.
 *
 * <p>The form now writes the lead down as soon as it has a name and an email, which means a row
 * can exist while the visitor is still answering. Such a row is <b>status NEW</b> - correctly,
 * because nobody has replied to it - and that is exactly what every work queue on this list
 * selects on. So without a guard, half-typed leads would be counted as unanswered, called stale
 * after three days, listed as having no phone number, and pushed into the urgent queue if they
 * happened to have named a date. The office would be chasing people who had not asked yet.
 *
 * <p>The guard is {@code submittedAt IS NOT NULL} on each of the four queues. It is checked here
 * by reading the predicate each specification builds, because a lifecycle marker that the queues
 * do not consult is decoration, and the failure is silent: the queue simply fills with rows that
 * do not belong in it, which looks like business rather than like a bug.
 */
class ADraftStaysOutOfTheSalesQueuesTest {

    /** The four queues that select on status, and therefore all need the guard. */
    private static final List<String> QUEUES =
        List.of("unanswered", "staleFor", "missingPhone", "travellingWithin");

    @Test
    @DisplayName("every work queue requires the lead to have been submitted")
    void theQueuesIgnoreWhatIsStillBeingTyped() throws Exception {
        for (String queue : QUEUES) {
            String rendered = predicateOf(queue);
            assertTrue(rendered.contains("submittedAt"),
                queue + "() does not require submittedAt, so a lead that is still being filled "
                    + "in will appear in that queue as though somebody were waiting on us: "
                    + rendered);
        }
    }

    @Test
    @DisplayName("submitted and partial are opposites, and neither is a status")
    void theTwoSidesOfTheLine() throws Exception {
        assertTrue(predicateOf("submitted").contains("isNotNull"),
            "submitted() must mean the form was finished");
        assertTrue(predicateOf("partial").contains("isNull"),
            "partial() must mean it never was");

        /*
         * The point of marking completion with a timestamp rather than a sixth InquiryStatus:
         * `status` is a native MySQL enum frozen at five values, it is spelled out again in the
         * panel and in a generated catalogue, and a half-written lead is not a stage of a sale.
         */
        assertTrue(java.util.Arrays.stream(
                com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus.values())
                .noneMatch(s -> s.name().contains("DRAFT") || s.name().contains("PARTIAL")),
            "A partial lead must not become a pipeline status");
    }

    @Test
    @DisplayName("asking for both sides, or neither, constrains nothing")
    void theFilterIsHonestAboutWhatItNarrows() {
        assertNull(BookingInquirySpecification.byCompletion(null));
        assertNull(BookingInquirySpecification.byCompletion(List.of()));
        assertNull(BookingInquirySpecification.byCompletion(List.of("complete", "partial")),
            "Both sides selected is the whole list, so it must add no constraint rather than "
                + "quietly returning nothing");
        assertNull(BookingInquirySpecification.byCompletion(List.of("nonsense")),
            "A value the API does not know narrows nothing, as the tracking dimension does");

        assertNotNull(BookingInquirySpecification.byCompletion(List.of("partial")));
        assertNotNull(BookingInquirySpecification.byCompletion(List.of("complete")));
    }

    @Test
    @DisplayName("the code is taken from the highest number used, not from a row count")
    void aDeletedInquiryDoesNotPoisonTheSequence() throws Exception {
        /*
         * Counting the month's rows and adding one is wrong the moment one is deleted: the count
         * drops by one and the next lead is handed a code that already exists, which then
         * collides on every attempt rather than once. The query reads the sequence back out of
         * the codes themselves instead.
         */
        Method query = com.itineraryledger.kabengosafaris.BookingInquiry.Repository
            .BookingInquiryRepository.class.getMethod("highestSequenceInMonth", String.class);
        assertNotNull(query, "highestSequenceInMonth must exist");
        assertEquals(int.class, query.getReturnType());

        var annotation = query.getAnnotation(org.springframework.data.jpa.repository.Query.class);
        assertNotNull(annotation, "it has to be a query, not a derived method name");
        assertTrue(annotation.value().contains("MAX("),
            "the sequence is the highest code issued, not how many rows there are: "
                + annotation.value());
    }

    /** What a specification's predicate looks like, which is the only way to read its intent. */
    private static String predicateOf(String name) throws Exception {
        Method method = java.util.Arrays.stream(BookingInquirySpecification.class.getMethods())
            .filter(m -> m.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "BookingInquirySpecification." + name + " is gone - has it been renamed?"));

        Object spec = method.getParameterCount() == 0
            ? method.invoke(null)
            : method.invoke(null, defaultArg(method.getParameterTypes()[0]));

        RecordingBuilder builder = new RecordingBuilder();
        java.lang.reflect.Method toPredicate = java.util.Arrays.stream(spec.getClass().getMethods())
            .filter(m -> m.getName().equals("toPredicate"))
            .findFirst().orElseThrow();
        toPredicate.setAccessible(true);
        toPredicate.invoke(spec, builder.root(), null, builder.criteriaBuilder());
        return builder.calls.toString();
    }

    private static Object defaultArg(Class<?> type) {
        if (type == int.class) return 3;
        return null;
    }

    /**
     * A root and a CriteriaBuilder that record what was asked of them.
     *
     * <p>Building a real EntityManager for this would turn a statement about four predicates
     * into a Spring context, a database and a schema. The predicates are the subject; what the
     * specification asks the builder for is enough to read them.
     */
    private static final class RecordingBuilder {
        private final StringBuilder calls = new StringBuilder();

        Object root() {
            return java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{jakarta.persistence.criteria.Root.class},
                (proxy, method, args) -> {
                    if ("get".equals(method.getName()) && args != null && args.length == 1) {
                        calls.append(" field:").append(args[0]);
                    }
                    return stub(method.getReturnType());
                });
        }

        Object criteriaBuilder() {
            return java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{jakarta.persistence.criteria.CriteriaBuilder.class},
                (proxy, method, args) -> {
                    calls.append(' ').append(method.getName());
                    return stub(method.getReturnType());
                });
        }

        private Object stub(Class<?> type) {
            if (type.isPrimitive()) return type == boolean.class ? Boolean.FALSE : 0;
            if (!type.isInterface()) return null;
            return java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{type},
                (p, m, a) -> {
                    calls.append(' ').append(m.getName());
                    return stub(m.getReturnType());
                });
        }
    }
}

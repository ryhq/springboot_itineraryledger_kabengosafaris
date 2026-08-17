package com.itineraryledger.kabengosafaris.Initializers;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Faq.Entity.Faq;
import com.itineraryledger.kabengosafaris.Faq.Repository.FaqRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Seeds ONE FAQ, in its own transaction — see BlogSeeder for why that matters. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FaqSeeder {

    private final FaqRepository faqRepository;

    /** @return true when an FAQ was written, false when the question was already there. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean seed(SeedFaq seed, int position) {
        String question = seed.q();
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("A seeded FAQ needs a question");
        }
        if (faqRepository.existsByQuestion(question)) {
            log.debug("FAQ already exists, leaving it alone: {}", question);
            return false;
        }

        faqRepository.save(Faq.builder()
            .question(question)
            .answer(seed.a())
            /*
             * No category: the site's FAQ page is one flat list and inventing groupings here
             * would be putting words in the owner's mouth. The field is there to be filled in.
             */
            .category(seed.category())
            .displayOrder(position)
            .isActive(true)
            .build());

        log.info("Seeded FAQ #{}: {}", position, question);
        return true;
    }

    /** One entry of seed/faqs.json — the website's FaqItem, verbatim. */
    public record SeedFaq(String q, String a, String category) {}
}

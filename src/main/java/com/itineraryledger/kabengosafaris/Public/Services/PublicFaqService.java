package com.itineraryledger.kabengosafaris.Public.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Faq.Entity.Faq;
import com.itineraryledger.kabengosafaris.Faq.Repository.FaqRepository;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicFaqDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** The global FAQ list, as the website's /faq page reads it: active only, in editorial order. */
@Service
@Slf4j
@RequiredArgsConstructor
public class PublicFaqService {

    private final FaqRepository faqRepository;
    private final PublicTranslationService publicTranslationService;

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getPublicFaqs(String lang) {
        try {
            List<Faq> faqs = faqRepository.findByIsActiveTrueOrderByDisplayOrderAscIdAsc();
            List<PublicFaqDTO> dtos = faqs.stream()
                .map(faq -> PublicFaqDTO.builder()
                    .q(faq.getQuestion())
                    .a(faq.getAnswer())
                    .category(faq.getCategory())
                    .build())
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(dtos, lang);
            return ResponseEntity.ok(ApiResponse.success(200, "FAQs retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error fetching public FAQs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch FAQs", "FAQS_FETCH_FAILED"));
        }
    }
}

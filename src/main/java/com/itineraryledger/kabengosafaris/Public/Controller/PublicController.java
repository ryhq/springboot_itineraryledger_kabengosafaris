package com.itineraryledger.kabengosafaris.Public.Controller;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Public.Services.PublicAccommodationService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicActivityService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicGalleryService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicHeroService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicHomepageService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicItineraryService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicNavigationService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicParkService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicSearchService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicTestimonyService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicTranslationService;
import com.itineraryledger.kabengosafaris.Newsletter.Services.NewsletterService;
import com.itineraryledger.kabengosafaris.Newsletter.DTOs.NewsletterSubscribeRequest;
import com.itineraryledger.kabengosafaris.BookingInquiry.Services.BookingInquiryService;
import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.BookingInquiryRequest;
import com.itineraryledger.kabengosafaris.ContactMessage.Services.ContactMessageService;
import com.itineraryledger.kabengosafaris.ContactMessage.DTOs.ContactMessageRequest;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicHomepageService publicHomepageService;
    private final PublicNavigationService publicNavigationService;
    private final PublicHeroService publicHeroService;
    private final PublicGalleryService publicGalleryService;
    private final PublicParkService publicParkService;
    private final PublicActivityService publicActivityService;
    private final PublicAccommodationService publicAccommodationService;
    private final PublicItineraryService publicItineraryService;
    private final PublicTestimonyService publicTestimonyService;
    private final PublicSearchService publicSearchService;
    private final PublicTranslationService publicTranslationService;
    private final NewsletterService newsletterService;
    private final BookingInquiryService bookingInquiryService;
    private final ContactMessageService contactMessageService;

    // ========================
    // SEARCH
    // ========================

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> search(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam String keyword,
        @RequestParam(defaultValue = "3") int limit
    ) {
        return publicSearchService.search(keyword, publicTranslationService.parseLanguage(lang), Math.min(limit, 20));
    }

    // ========================
    // HOMEPAGE (aggregated)
    // ========================

    @GetMapping("/homepage")
    public ResponseEntity<ApiResponse<?>> getHomepageData(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang
    ) {
        return publicHomepageService.getHomepageData(publicTranslationService.parseLanguage(lang));
    }

    // ========================
    // NAVIGATION
    // ========================

    @GetMapping("/navigation")
    public ResponseEntity<ApiResponse<?>> getNavigation(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang
    ) {
        return publicNavigationService.getNavigation(publicTranslationService.parseLanguage(lang));
    }

    // ========================
    // PARKS
    // ========================

    @GetMapping("/parks")
    public ResponseEntity<ApiResponse<?>> getParks(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) ParkType parkType,
        @RequestParam(required = false) String keyword
    ) {
        return publicParkService.getParks(page, size, sortBy, sortDirection, region, parkType, keyword, publicTranslationService.parseLanguage(lang));
    }

    @GetMapping("/parks/{identifier}")
    public ResponseEntity<ApiResponse<?>> getPark(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @PathVariable String identifier
    ) {
        return publicParkService.getParkByIdentifier(identifier, publicTranslationService.parseLanguage(lang));
    }

    @GetMapping("/parks/{identifier}/images")
    public ResponseEntity<ApiResponse<?>> getParkImages(
        @PathVariable String identifier,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicParkService.getParkImages(identifier, page, size);
    }

    @GetMapping("/parks/{identifier}/activities")
    public ResponseEntity<ApiResponse<?>> getParkActivities(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @PathVariable String identifier,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicParkService.getParkActivities(identifier, page, size, publicTranslationService.parseLanguage(lang));
    }

    // ========================
    // ACTIVITIES
    // ========================

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<?>> getActivities(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) String keyword
    ) {
        return publicActivityService.getActivities(page, size, sortBy, sortDirection, keyword, publicTranslationService.parseLanguage(lang));
    }

    @GetMapping("/activities/{identifier}")
    public ResponseEntity<ApiResponse<?>> getActivity(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @PathVariable String identifier
    ) {
        return publicActivityService.getActivityByIdentifier(identifier, publicTranslationService.parseLanguage(lang));
    }

    @GetMapping("/activities/{identifier}/images")
    public ResponseEntity<ApiResponse<?>> getActivityImages(
        @PathVariable String identifier,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicActivityService.getActivityImages(identifier, page, size);
    }

    @GetMapping("/activities/{identifier}/parks")
    public ResponseEntity<ApiResponse<?>> getActivityParks(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @PathVariable String identifier,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicActivityService.getActivityParks(identifier, page, size, publicTranslationService.parseLanguage(lang));
    }

    // ========================
    // ACCOMMODATIONS
    // ========================

    @GetMapping("/accommodations")
    public ResponseEntity<ApiResponse<?>> getAccommodations(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) AccommodationType type,
        @RequestParam(required = false) AccommodationCategory category,
        @RequestParam(required = false) String keyword
    ) {
        return publicAccommodationService.getAccommodations(page, size, sortBy, sortDirection, region, type, category, keyword, publicTranslationService.parseLanguage(lang));
    }

    @GetMapping("/accommodations/{identifier}")
    public ResponseEntity<ApiResponse<?>> getAccommodation(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @PathVariable String identifier
    ) {
        return publicAccommodationService.getAccommodationByIdentifier(identifier, publicTranslationService.parseLanguage(lang));
    }

    @GetMapping("/accommodations/{identifier}/images")
    public ResponseEntity<ApiResponse<?>> getAccommodationImages(
        @PathVariable String identifier,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicAccommodationService.getAccommodationImages(identifier, page, size);
    }

    // ========================
    // SAFARIS (Itineraries exposed as Safaris)
    // ========================

    @GetMapping("/safaris")
    public ResponseEntity<ApiResponse<?>> getSafaris(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) TripType tripType,
        @RequestParam(required = false) BudgetCategory budgetCategory,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer minDays,
        @RequestParam(required = false) Integer maxDays
    ) {
        return publicItineraryService.getItineraries(page, size, sortBy, sortDirection, tripType, budgetCategory, keyword, minDays, maxDays, publicTranslationService.parseLanguage(lang));
    }

    @GetMapping("/safaris/{identifier}")
    public ResponseEntity<ApiResponse<?>> getSafari(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @PathVariable String identifier
    ) {
        return publicItineraryService.getItineraryByIdentifier(identifier, publicTranslationService.parseLanguage(lang));
    }

    // ========================
    // HEROES
    // ========================

    @GetMapping("/heroes")
    public ResponseEntity<ApiResponse<?>> getHeroesByPage(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam HeroPage heroPage
    ) {
        return publicHeroService.getHeroesByPage(heroPage, publicTranslationService.parseLanguage(lang));
    }

    // ========================
    // GALLERY (unified images)
    // ========================

    @GetMapping("/gallery")
    public ResponseEntity<ApiResponse<?>> getGalleryImages(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicGalleryService.getGalleryImages(entityType, page, size, publicTranslationService.parseLanguage(lang));
    }

    // ========================
    // TESTIMONIES
    // ========================

    @GetMapping("/testimonies")
    public ResponseEntity<ApiResponse<?>> getTestimonies(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        String parsedLang = publicTranslationService.parseLanguage(lang);
        if (page != null || size != null) {
            return publicTestimonyService.getPublicTestimoniesPaginated(page, size, parsedLang);
        }
        return publicTestimonyService.getPublicTestimonies(parsedLang);
    }

    @GetMapping("/testimonies/featured")
    public ResponseEntity<ApiResponse<?>> getFeaturedTestimonies(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang
    ) {
        return publicTestimonyService.getFeaturedTestimonies(publicTranslationService.parseLanguage(lang));
    }

    @PostMapping("/testimonies")
    public ResponseEntity<ApiResponse<?>> submitPublicTestimony(
        @RequestBody PublicTestimonyService.PublicTestimonyRequest request
    ) {
        return publicTestimonyService.submitPublicTestimony(request);
    }

    // ========================
    // NEWSLETTER
    // ========================

    @PostMapping("/newsletter/subscribe")
    public ResponseEntity<Map<String, Object>> subscribeToNewsletter(
            @Valid @RequestBody NewsletterSubscribeRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String langHeader) {
        String lang = publicTranslationService.parseLanguage(langHeader);
        if (request.getLocale() == null || request.getLocale().isBlank()) {
            request.setLocale(lang);
        }
        Map<String, Object> result = newsletterService.subscribe(request);
        return ResponseEntity.ok(result);
    }

    // ========================
    // BOOKING INQUIRIES
    // ========================

    @PostMapping("/booking-inquiries")
    public ResponseEntity<Map<String, Object>> submitBookingInquiry(
            @Valid @RequestBody BookingInquiryRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String langHeader) {
        String lang = publicTranslationService.parseLanguage(langHeader);
        if (request.getLocale() == null || request.getLocale().isBlank()) {
            request.setLocale(lang);
        }
        Map<String, Object> result = bookingInquiryService.submitInquiry(request);
        return ResponseEntity.ok(result);
    }

    // ========================
    // TRANSLATION LANGUAGES
    // ========================

    @GetMapping("/translation/languages")
    public ResponseEntity<ApiResponse<?>> getSupportedLanguages() {
        return publicTranslationService.getSupportedLanguages();
    }

    @PostMapping("/translation/translate-messages")
    public ResponseEntity<ApiResponse<?>> translateMessages(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) request.get("texts");
        String targetLanguage = (String) request.get("targetLanguage");
        return publicTranslationService.translateMessages(texts, targetLanguage);
    }

    // ========================
    // CONTACT US
    // ========================

    @PostMapping("/contact")
    public ResponseEntity<Map<String, Object>> submitContactMessage(
            @Valid @RequestBody ContactMessageRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String langHeader) {
        String lang = publicTranslationService.parseLanguage(langHeader);
        if (request.getLocale() == null || request.getLocale().isBlank()) {
            request.setLocale(lang);
        }
        Map<String, Object> result = contactMessageService.submitContactMessage(request);
        return ResponseEntity.ok(result);
    }
}

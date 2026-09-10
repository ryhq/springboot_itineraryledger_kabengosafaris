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
import com.itineraryledger.kabengosafaris.Public.Services.PublicBlogService;
import com.itineraryledger.kabengosafaris.Public.Services.PublicFaqService;
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
import org.springframework.http.MediaType;
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
    private final PublicBlogService publicBlogService;
    private final PublicFaqService publicFaqService;
    private final PublicSearchService publicSearchService;
    private final PublicTranslationService publicTranslationService;
    private final NewsletterService newsletterService;
    private final BookingInquiryService bookingInquiryService;
    private final ContactMessageService contactMessageService;
    private final com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService company;

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
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) java.util.List<String> tag
    ) {
        return publicParkService.getParks(page, size, sortBy, sortDirection, region, parkType, keyword, tag, publicTranslationService.parseLanguage(lang));
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

    @GetMapping("/parks/{identifier}/safaris")
    public ResponseEntity<ApiResponse<?>> getParkSafaris(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @PathVariable String identifier,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicItineraryService.getParkSafaris(identifier, page, size, publicTranslationService.parseLanguage(lang));
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

    @GetMapping("/activities/{identifier}/safaris")
    public ResponseEntity<ApiResponse<?>> getActivitySafaris(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @PathVariable String identifier,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicItineraryService.getActivitySafaris(identifier, page, size, publicTranslationService.parseLanguage(lang));
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

    @GetMapping("/accommodations/{identifier}/safaris")
    public ResponseEntity<ApiResponse<?>> getAccommodationSafaris(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @PathVariable String identifier,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicItineraryService.getAccommodationSafaris(identifier, page, size, publicTranslationService.parseLanguage(lang));
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

    /** Most-booked safaris = itineraries converted into the most actual Safaris. */
    @GetMapping("/safaris/popular")
    public ResponseEntity<ApiResponse<?>> getPopularSafaris(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam(required = false) Integer size
    ) {
        return publicItineraryService.getMostBooked(size, publicTranslationService.parseLanguage(lang));
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

    // ========================
    // BLOG
    // ========================

    /**
     * The blog index.
     *
     * Paged when asked (page/size) and whole otherwise, matching the testimony endpoint's
     * behaviour — the site's index wants a page, its sitemap and llms.txt want all of them.
     */
    @GetMapping("/blogs")
    public ResponseEntity<ApiResponse<?>> getBlogs(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return publicBlogService.getPublicBlogs(page, size, publicTranslationService.parseLanguage(lang));
    }

    /** One article, by the slug the site serves it at. */
    @GetMapping("/blogs/{slug}")
    public ResponseEntity<ApiResponse<?>> getBlogBySlug(
        @PathVariable String slug,
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang
    ) {
        return publicBlogService.getPublicBlogBySlug(slug, publicTranslationService.parseLanguage(lang));
    }

    // ========================
    // FAQ
    // ========================

    /** The global FAQ list for /faq — active only, in the order the panel set. */
    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<?>> getFaqs(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang
    ) {
        return publicFaqService.getPublicFaqs(publicTranslationService.parseLanguage(lang));
    }

    @GetMapping("/testimonies/summary")
    public ResponseEntity<ApiResponse<?>> getTestimonySummary() {
        return publicTestimonyService.getTestimonySummary();
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

    /**
     * The click at the end of a confirmation email.
     *
     * <p>Answers with a page rather than JSON, because the thing following this link is a person
     * in their inbox, not the website. The website is a separate application whose routes this
     * service cannot know, and a confirmation link is the one link that must work first time: it
     * is all that stands between a typed address and a list we are allowed to write to. A website
     * that wants to own the page can call the POST below instead.
     */
    @GetMapping(value = "/newsletter/confirm", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmNewsletterPage(@RequestParam(required = false) String token) {
        Map<String, Object> result = newsletterService.confirm(token);
        String status = String.valueOf(result.get("status"));
        boolean good = "confirmed".equals(status) || "already_confirmed".equals(status);
        return ResponseEntity.ok(page(
            good ? "You are on the list" : "That link did not work",
            String.valueOf(result.get("message"))));
    }

    /** The same, for a website that would rather render its own page. */
    @PostMapping("/newsletter/confirm")
    public ResponseEntity<Map<String, Object>> confirmNewsletter(@RequestParam(required = false) String token) {
        return ResponseEntity.ok(newsletterService.confirm(token));
    }

    /**
     * One-click unsubscribe from the link at the foot of a newsletter.
     *
     * <p>Takes the subscription's token, not an email address. Unsubscribing by address lets
     * anybody remove anybody, and it also turns the endpoint into a way of asking whether a given
     * address is on the list.
     */
    @GetMapping(value = "/newsletter/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribeNewsletterPage(@RequestParam(required = false) String token) {
        Map<String, Object> result = newsletterService.unsubscribeByToken(token);
        return ResponseEntity.ok(page("You have been removed", String.valueOf(result.get("message"))));
    }

    @PostMapping("/newsletter/unsubscribe-by-token")
    public ResponseEntity<Map<String, Object>> unsubscribeNewsletterByToken(
            @RequestParam(required = false) String token) {
        return ResponseEntity.ok(newsletterService.unsubscribeByToken(token));
    }

    /**
     * One small page, in the company's own colour, for the two links a subscriber clicks.
     *
     * <p>Built here rather than as a template because it has no data in it worth a template and
     * must never fail to render: it is the page somebody sees at the end of the only email that
     * can get them onto the list.
     */
    private String page(String heading, String message) {
        Map<String, String> brand = company.variables();
        String name = escape(brand.getOrDefault("companyName", ""));
        String accent = escape(brand.getOrDefault("companyAccentDark", "#1f2421"));
        String website = escape(brand.getOrDefault("companyWebsite", ""));
        return """
            <!doctype html><html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>%s</title></head>
            <body style="margin:0;background:#f6f7f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;color:#1f2421">
              <div style="max-width:520px;margin:0 auto;padding:56px 20px">
                <div style="background:#fff;border-radius:10px;padding:32px 28px;border-top:4px solid %s">
                  <h1 style="margin:0 0 10px;font-size:21px;line-height:1.3">%s</h1>
                  <p style="margin:0;font-size:15px;line-height:1.6;color:#374151">%s</p>
                  %s
                </div>
                <p style="margin:18px 0 0;text-align:center;font-size:12px;color:#6b7280">%s</p>
              </div>
            </body></html>
            """.formatted(
                escape(heading), accent, escape(heading), escape(message),
                website.isBlank() ? "" :
                    "<p style=\"margin:18px 0 0;font-size:15px\"><a href=\"" + website
                        + "\" style=\"color:" + accent + "\">Back to our safaris</a></p>",
                name);
    }

    /** No value here is ours, so none of it goes into the page unescaped. */
    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
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

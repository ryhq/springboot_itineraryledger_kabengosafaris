package com.itineraryledger.kabengosafaris.Public.Controller;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Public.Services.PublicService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public Website API", description = "Unauthenticated APIs for the public website frontend")
public class PublicController {

    private final PublicService publicService;

    // ========================
    // NAVIGATION
    // ========================

    @GetMapping("/navigation")
    public ResponseEntity<ApiResponse<?>> getNavigation() {
        return publicService.getNavigation();
    }

    // ========================
    // PARKS
    // ========================

    @GetMapping("/parks")
    public ResponseEntity<ApiResponse<?>> getParks(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) ParkType parkType,
        @RequestParam(required = false) String keyword
    ) {
        return publicService.getParks(page, size, sortBy, sortDirection, region, parkType, keyword);
    }

    @GetMapping("/parks/id/{id}")
    public ResponseEntity<ApiResponse<?>> getParkById(@PathVariable String id) {
        return publicService.getParkById(id);
    }

    @GetMapping("/parks/{slug}")
    public ResponseEntity<ApiResponse<?>> getParkBySlug(@PathVariable String slug) {
        return publicService.getParkBySlug(slug);
    }

    // ========================
    // ACTIVITIES
    // ========================

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<?>> getActivities(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) String keyword
    ) {
        return publicService.getActivities(page, size, sortBy, sortDirection, keyword);
    }

    @GetMapping("/activities/id/{id}")
    public ResponseEntity<ApiResponse<?>> getActivityById(@PathVariable String id) {
        return publicService.getActivityById(id);
    }

    @GetMapping("/activities/{slug}")
    public ResponseEntity<ApiResponse<?>> getActivityBySlug(@PathVariable String slug) {
        return publicService.getActivityBySlug(slug);
    }

    // ========================
    // ACCOMMODATIONS
    // ========================

    @GetMapping("/accommodations")
    public ResponseEntity<ApiResponse<?>> getAccommodations(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) AccommodationType type,
        @RequestParam(required = false) AccommodationCategory category,
        @RequestParam(required = false) String keyword
    ) {
        return publicService.getAccommodations(page, size, sortBy, sortDirection, region, type, category, keyword);
    }

    @GetMapping("/accommodations/id/{id}")
    public ResponseEntity<ApiResponse<?>> getAccommodationById(@PathVariable String id) {
        return publicService.getAccommodationById(id);
    }

    @GetMapping("/accommodations/{slug}")
    public ResponseEntity<ApiResponse<?>> getAccommodationBySlug(@PathVariable String slug) {
        return publicService.getAccommodationBySlug(slug);
    }

    // ========================
    // ITINERARIES
    // ========================

    @GetMapping("/itineraries")
    public ResponseEntity<ApiResponse<?>> getItineraries(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) TripType tripType,
        @RequestParam(required = false) BudgetCategory budgetCategory,
        @RequestParam(required = false) String keyword
    ) {
        return publicService.getItineraries(page, size, sortBy, sortDirection, tripType, budgetCategory, keyword);
    }

    @GetMapping("/itineraries/id/{id}")
    public ResponseEntity<ApiResponse<?>> getItineraryById(@PathVariable String id) {
        return publicService.getItineraryById(id);
    }

    // ========================
    // SAFARIS
    // ========================

    @GetMapping("/safaris")
    public ResponseEntity<ApiResponse<?>> getSafaris(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection,
        @RequestParam(required = false) String keyword
    ) {
        return publicService.getSafaris(page, size, sortBy, sortDirection, keyword);
    }

    @GetMapping("/safaris/{slug}")
    public ResponseEntity<ApiResponse<?>> getSafariBySlug(@PathVariable String slug) {
        return publicService.getSafariBySlug(slug);
    }

    // ========================
    // TESTIMONIES
    // ========================

    @GetMapping("/testimonies")
    public ResponseEntity<ApiResponse<?>> getTestimonies() {
        return publicService.getTestimonies();
    }

    @GetMapping("/testimonies/featured")
    public ResponseEntity<ApiResponse<?>> getFeaturedTestimonies() {
        return publicService.getFeaturedTestimonies();
    }

    // ========================
    // HEROES
    // ========================

    @GetMapping("/heroes/{page}")
    public ResponseEntity<ApiResponse<?>> getHeroesByPage(@PathVariable HeroPage page) {
        return publicService.getHeroesByPage(page);
    }
}

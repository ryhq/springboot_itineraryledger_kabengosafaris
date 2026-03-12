package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility for resolving entities by identifier (id, slug, or code).
 * Tries: decode as obfuscated ID → slug → code (where applicable).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PublicEntityResolver {

    private final IdObfuscator idObfuscator;
    private final ParkRepository parkRepository;
    private final ActivityRepository activityRepository;
    private final AccommodationRepository accommodationRepository;
    private final ItineraryRepository itineraryRepository;

    /**
     * Resolve Park by identifier: id → slug
     */
    public Optional<Park> resolvePark(String identifier) {
        // Try decode as obfuscated ID
        try {
            Long id = idObfuscator.decodeId(identifier);
            Optional<Park> byId = parkRepository.findById(id);
            if (byId.isPresent()) return byId;
        } catch (Exception ignored) {}

        // Try slug
        return parkRepository.findBySlug(identifier);
    }

    /**
     * Resolve Activity by identifier: id → slug
     */
    public Optional<Activity> resolveActivity(String identifier) {
        try {
            Long id = idObfuscator.decodeId(identifier);
            Optional<Activity> byId = activityRepository.findById(id);
            if (byId.isPresent()) return byId;
        } catch (Exception ignored) {}

        return activityRepository.findBySlug(identifier);
    }

    /**
     * Resolve Accommodation by identifier: id → slug
     */
    public Optional<Accommodation> resolveAccommodation(String identifier) {
        try {
            Long id = idObfuscator.decodeId(identifier);
            Optional<Accommodation> byId = accommodationRepository.findById(id);
            if (byId.isPresent()) return byId;
        } catch (Exception ignored) {}

        return accommodationRepository.findBySlug(identifier);
    }

    /**
     * Resolve Itinerary by identifier: id → code
     */
    public Optional<Itinerary> resolveItinerary(String identifier) {
        try {
            Long id = idObfuscator.decodeId(identifier);
            Optional<Itinerary> byId = itineraryRepository.findById(id);
            if (byId.isPresent()) return byId;
        } catch (Exception ignored) {}

        return itineraryRepository.findByCode(identifier);
    }
}

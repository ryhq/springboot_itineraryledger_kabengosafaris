package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Repository.ItineraryImageRepository;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicImageDTO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Resolves primary images for entities using the isPrimary flag on image entities.
 * Falls back to random image selection when no primary is set.
 */
@Component
@Slf4j
public class PublicImageResolver {

    private final ParkImageRepository parkImageRepository;
    private final ActivityImageRepository activityImageRepository;
    private final AccommodationImageRepository accommodationImageRepository;
    private final ItineraryImageRepository itineraryImageRepository;
    private final String appBaseUrl;

    public PublicImageResolver(
            ParkImageRepository parkImageRepository,
            ActivityImageRepository activityImageRepository,
            AccommodationImageRepository accommodationImageRepository,
            ItineraryImageRepository itineraryImageRepository,
            @Value("${app.base.url}") String appBaseUrl) {
        this.parkImageRepository = parkImageRepository;
        this.activityImageRepository = activityImageRepository;
        this.accommodationImageRepository = accommodationImageRepository;
        this.itineraryImageRepository = itineraryImageRepository;
        this.appBaseUrl = appBaseUrl;
    }

    /**
     * Resolve the itinerary's own uploaded hero image (Option B).
     * Priority: the primary image → the first active image → null.
     * Callers should try this first, then fall back to the deterministic
     * park/activity pick ({@link #resolveSafariImageDeterministic}).
     */
    public String resolveItineraryHero(Long itineraryId) {
        if (itineraryId == null) return null;
        try {
            Optional<ItineraryImage> primary = itineraryImageRepository.findPrimaryByItineraryId(itineraryId);
            if (primary.isPresent() && primary.get().getFileName() != null) {
                return toFullImageUrl("/api/itinerary-images/file/" + primary.get().getFileName());
            }
            List<ItineraryImage> active = itineraryImageRepository.findActiveByItineraryId(itineraryId);
            if (!active.isEmpty() && active.get(0).getFileName() != null) {
                return toFullImageUrl("/api/itinerary-images/file/" + active.get(0).getFileName());
            }
        } catch (Exception e) {
            log.debug("Could not resolve itinerary hero image for itinerary {}", itineraryId);
        }
        return null;
    }

    /**
     * Get primary image URL for a park.
     * Priority: isPrimary image → entity primaryImage field → random active image → null
     */
    public String resolveParkImage(Long parkId, String entityPrimaryImage) {
        // 1. Check isPrimary flag on park images
        try {
            Optional<ParkImage> primary = parkImageRepository.findPrimaryByParkId(parkId);
            if (primary.isPresent() && primary.get().getFileName() != null) {
                return toFullImageUrl("/api/park-images/file/" + primary.get().getFileName());
            }
        } catch (Exception e) {
            log.debug("Could not check primary park image for park {}", parkId);
        }

        // 2. Fall back to entity's primaryImage field
        String resolved = toFullImageUrl(entityPrimaryImage);
        if (resolved != null) return resolved;

        // 3. Fall back to random active image
        try {
            List<ParkImage> activeImages = parkImageRepository.findActiveByParkId(parkId);
            if (!activeImages.isEmpty()) {
                ParkImage random = activeImages.get(ThreadLocalRandom.current().nextInt(activeImages.size()));
                if (random.getFileName() != null) {
                    return toFullImageUrl("/api/park-images/file/" + random.getFileName());
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch park images for park {}", parkId);
        }

        return null;
    }

    /**
     * Get primary image URL for an activity.
     * Priority: isPrimary image → entity primaryImage field → random active image → null
     */
    public String resolveActivityImage(Long activityId, String entityPrimaryImage) {
        // 1. Check isPrimary flag
        try {
            Optional<ActivityImage> primary = activityImageRepository.findByActivityIdAndIsPrimaryTrue(activityId);
            if (primary.isPresent() && primary.get().getFileName() != null) {
                return toFullImageUrl("/api/activity-images/file/" + primary.get().getFileName());
            }
        } catch (Exception e) {
            log.debug("Could not check primary activity image for activity {}", activityId);
        }

        // 2. Fall back to entity's primaryImage field
        String resolved = toFullImageUrl(entityPrimaryImage);
        if (resolved != null) return resolved;

        // 3. Fall back to random active image
        try {
            List<ActivityImage> activeImages = activityImageRepository
                .findByActivityIdAndIsActiveOrderByDisplayOrderAsc(activityId, true);
            if (!activeImages.isEmpty()) {
                ActivityImage random = activeImages.get(ThreadLocalRandom.current().nextInt(activeImages.size()));
                if (random.getFileName() != null) {
                    return toFullImageUrl("/api/activity-images/file/" + random.getFileName());
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch activity images for activity {}", activityId);
        }

        return null;
    }

    /**
     * Get primary image URL for an accommodation.
     * Priority: isPrimary image → random active image → null
     */
    public String resolveAccommodationImage(Long accommodationId) {
        // 1. Check isPrimary flag
        try {
            Optional<AccommodationImage> primary = accommodationImageRepository
                .findPrimaryByAccommodationId(accommodationId);
            if (primary.isPresent() && primary.get().getFileName() != null) {
                return toFullImageUrl("/api/accommodation-images/file/" + primary.get().getFileName());
            }
        } catch (Exception e) {
            log.debug("Could not check primary accommodation image for accommodation {}", accommodationId);
        }

        // 2. Fall back to random active image
        try {
            List<AccommodationImage> activeImages = accommodationImageRepository
                .findActiveByAccommodationId(accommodationId);
            if (!activeImages.isEmpty()) {
                AccommodationImage random = activeImages.get(ThreadLocalRandom.current().nextInt(activeImages.size()));
                if (random.getFileName() != null) {
                    return toFullImageUrl("/api/accommodation-images/file/" + random.getFileName());
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch accommodation images for accommodation {}", accommodationId);
        }

        return null;
    }

    /**
     * Resolve primary image for a safari.
     * Priority: parks with primary images > activities with primary images > accommodations with primary images
     * Fallback: random image from all linked entities.
     */
    public String resolveSafariImage(List<Long> parkIds, List<Long> activityIds, List<Long> accommodationIds,
                                      List<String> parkEntityImages, List<String> activityEntityImages) {
        // 1. Check parks for primary images
        if (parkIds != null) {
            for (int i = 0; i < parkIds.size(); i++) {
                String img = resolveParkImage(parkIds.get(i),
                    parkEntityImages != null && i < parkEntityImages.size() ? parkEntityImages.get(i) : null);
                if (img != null) return img;
            }
        }

        // 2. Check activities for primary images
        if (activityIds != null) {
            for (int i = 0; i < activityIds.size(); i++) {
                String img = resolveActivityImage(activityIds.get(i),
                    activityEntityImages != null && i < activityEntityImages.size() ? activityEntityImages.get(i) : null);
                if (img != null) return img;
            }
        }

        // 3. Check accommodations for primary images
        if (accommodationIds != null) {
            for (Long accId : accommodationIds) {
                String img = resolveAccommodationImage(accId);
                if (img != null) return img;
            }
        }

        return null;
    }

    /**
     * Deterministic per-safari image pick. Builds the pool of images from the trip's
     * linked entities and selects one by a stable hash of the safari/itinerary id, so
     * two trips that share the same first park no longer show the same photo — and the
     * chosen image never changes between requests. Prefers scenic park/activity images
     * over accommodation (lodge-room) shots.
     */
    public String resolveSafariImageDeterministic(long seed,
                                                  List<Long> parkIds, List<Long> activityIds, List<Long> accommodationIds,
                                                  List<String> parkEntityImages, List<String> activityEntityImages) {
        // 1. Scenic pool: parks + activities
        String scenic = pickBySeed(seed, collectDayImages(parkIds, activityIds, null, parkEntityImages, activityEntityImages));
        if (scenic != null) return scenic;
        // 2. Fall back to accommodation images
        return pickBySeed(seed, collectDayImages(null, null, accommodationIds, null, null));
    }

    /** Stable pick: sort the pool so DB row order can't change the result, then index by seed. */
    private String pickBySeed(long seed, List<String> images) {
        if (images == null || images.isEmpty()) return null;
        List<String> sorted = images.stream().sorted().collect(Collectors.toList());
        return sorted.get((int) Math.floorMod(seed, sorted.size()));
    }

    /**
     * Collect all available image URLs from parks, activities, and accommodations for an itinerary day.
     */
    public List<String> collectDayImages(List<Long> parkIds, List<Long> activityIds, List<Long> accommodationIds,
                                          List<String> parkEntityImages, List<String> activityEntityImages) {
        List<String> pool = new ArrayList<>();

        if (parkIds != null) {
            for (int i = 0; i < parkIds.size(); i++) {
                String entityImg = parkEntityImages != null && i < parkEntityImages.size() ? parkEntityImages.get(i) : null;
                String resolved = toFullImageUrl(entityImg);
                if (resolved != null) pool.add(resolved);
                try {
                    parkImageRepository.findActiveByParkId(parkIds.get(i)).forEach(img -> {
                        if (img.getFileName() != null && !img.getFileName().isBlank()) {
                            pool.add(toFullImageUrl("/api/park-images/file/" + img.getFileName()));
                        }
                    });
                } catch (Exception ignored) {}
            }
        }

        if (activityIds != null) {
            for (int i = 0; i < activityIds.size(); i++) {
                String entityImg = activityEntityImages != null && i < activityEntityImages.size() ? activityEntityImages.get(i) : null;
                String resolved = toFullImageUrl(entityImg);
                if (resolved != null) pool.add(resolved);
                try {
                    activityImageRepository.findByActivityIdAndIsActiveOrderByDisplayOrderAsc(activityIds.get(i), true)
                        .forEach(img -> {
                            if (img.getFileName() != null && !img.getFileName().isBlank()) {
                                pool.add(toFullImageUrl("/api/activity-images/file/" + img.getFileName()));
                            }
                        });
                } catch (Exception ignored) {}
            }
        }

        if (accommodationIds != null) {
            for (Long accId : accommodationIds) {
                try {
                    accommodationImageRepository.findActiveByAccommodationId(accId).forEach(img -> {
                        if (img.getFileName() != null && !img.getFileName().isBlank()) {
                            pool.add(toFullImageUrl("/api/accommodation-images/file/" + img.getFileName()));
                        }
                    });
                } catch (Exception ignored) {}
            }
        }

        return pool.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Pick a random image from a list, or null if empty.
     */
    public String pickRandom(List<String> images) {
        if (images == null || images.isEmpty()) return null;
        return images.get(ThreadLocalRandom.current().nextInt(images.size()));
    }

    /**
     * Build a full image URL from a relative path.
     */
    public String toFullImageUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) return relativePath;
        return appBaseUrl + (relativePath.startsWith("/") ? "" : "/") + relativePath;
    }

    // ── Public DTO converters ────────────────────────────────────────

    public PublicImageDTO toPublicDTO(ParkImage image) {
        return PublicImageDTO.builder()
            .imageUrl(toFullImageUrl("/api/park-images/file/" + image.getFileName()))
            .altText(image.getAltText())
            .caption(image.getCaption())
            .imageType(image.getImageType() != null ? image.getImageType().getDisplayName() : null)
            .build();
    }

    public PublicImageDTO toPublicDTO(ActivityImage image) {
        return PublicImageDTO.builder()
            .imageUrl(toFullImageUrl("/api/activity-images/file/" + image.getFileName()))
            .altText(image.getAltText())
            .caption(image.getCaption())
            .imageType(image.getImageType() != null ? image.getImageType().getDisplayName() : null)
            .build();
    }

    public PublicImageDTO toPublicDTO(AccommodationImage image) {
        return PublicImageDTO.builder()
            .imageUrl(toFullImageUrl("/api/accommodation-images/file/" + image.getFileName()))
            .altText(image.getAltText())
            .caption(image.getCaption())
            .imageType(image.getImageType() != null ? image.getImageType().getDisplayName() : null)
            .build();
    }
}

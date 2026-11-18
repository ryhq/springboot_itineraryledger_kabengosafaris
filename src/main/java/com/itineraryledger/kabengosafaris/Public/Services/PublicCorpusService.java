package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.AccommodationSpecification;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ActivitySpecification;
import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository;
import com.itineraryledger.kabengosafaris.Blog.Specifications.BlogSpecification;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Specifications.ItinerarySpecification;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkSpecification;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicCorpusDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicParkFeeDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The whole published catalogue in one response, for the website's {@code /llms-full.txt}.
 *
 * <p>See {@link PublicCorpusDTO} for why this endpoint exists at all. The short version: the
 * list endpoints omit the prose on purpose, so a corpus assembled from them needs a detail
 * request per record - 194 of them - and that is the wrong shape for a file a crawler reads.
 *
 * <p>Every record is converted by the <b>same converter its own detail endpoint uses</b>. That
 * is the whole design: a park must not be able to read one way in the corpus and another way on
 * its page, and the only way to guarantee that is to have one converter, not two.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicCorpusService {

    private final ParkRepository parkRepository;
    private final AccommodationRepository accommodationRepository;
    private final ActivityRepository activityRepository;
    private final ItineraryRepository itineraryRepository;
    private final BlogRepository blogRepository;
    private final ParkTariffRateRepository parkTariffRateRepository;

    private final PublicParkService publicParkService;
    private final PublicAccommodationService publicAccommodationService;
    private final PublicActivityService publicActivityService;
    private final PublicItineraryService publicItineraryService;
    private final PublicBlogService publicBlogService;

    /** The one fee a traveller actually asks about. */
    private static final String CONSERVATION_FEE = "Conservation Fee";
    private static final String NON_RESIDENT = "Non-Resident";
    private static final String ADULT = "Adult";

    /**
     * Read-only and in one transaction, because the itinerary converter walks lazy day
     * collections and would otherwise hit a closed session partway through 69 trips.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getCorpus() {
        try {
            PublicCorpusDTO corpus = PublicCorpusDTO.builder()
                .generatedAt(Instant.now().toString())
                .language("en")
                .parks(parks())
                .parkFees(parkFees())
                .accommodations(accommodations())
                .activities(activities())
                .safaris(safaris())
                .articles(articles())
                .build();

            return ResponseEntity.ok(ApiResponse.success(200, "Corpus retrieved", corpus));
        } catch (Exception e) {
            log.error("Error building public corpus", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to build corpus", "CORPUS_BUILD_FAILED"));
        }
    }

    // ========================
    // COLLECTIONS
    // ========================

    private List<com.itineraryledger.kabengosafaris.Public.DTOs.PublicParkDetailDTO> parks() {
        Specification<Park> spec = ParkSpecification.isActive(true)
            .and(ParkSpecification.isWebActive(true));
        return parkRepository.findAll(spec, Sort.by("name").ascending()).stream()
            .map(publicParkService::convertToDetailDTO)
            .collect(Collectors.toList());
    }

    private List<com.itineraryledger.kabengosafaris.Public.DTOs.PublicAccommodationDTO> accommodations() {
        Specification<Accommodation> spec = AccommodationSpecification.isActive(true)
            .and(AccommodationSpecification.isWebActive(true));
        return accommodationRepository.findAll(spec, Sort.by("name").ascending()).stream()
            .map(publicAccommodationService::convertToDetailDTO)
            .collect(Collectors.toList());
    }

    private List<com.itineraryledger.kabengosafaris.Public.DTOs.PublicActivityDetailDTO> activities() {
        Specification<Activity> spec = ActivitySpecification.isActive(true)
            .and(ActivitySpecification.isWebActive(true));
        return activityRepository.findAll(spec, Sort.by("name").ascending()).stream()
            .map(publicActivityService::convertToDetailDTO)
            .collect(Collectors.toList());
    }

    /**
     * Unlike the other four, this does NOT call a converter directly: a trip's cost summary is
     * attached by the caller of that converter, so {@code corpusDetails} does both, in one query
     * for all of them. Calling the converter here would have published every price as absent.
     */
    private List<com.itineraryledger.kabengosafaris.Public.DTOs.PublicItineraryDTO> safaris() {
        Specification<Itinerary> spec = ItinerarySpecification.isActiveAndPublished();
        return publicItineraryService.corpusDetails(
            itineraryRepository.findAll(spec, Sort.by("totalDays").ascending()));
    }

    private List<com.itineraryledger.kabengosafaris.Public.DTOs.PublicBlogDetailDTO> articles() {
        Specification<Blog> spec = Specification.<Blog>unrestricted()
            .and(BlogSpecification.isPublished(true));
        Sort sort = Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("publishDate"));
        return blogRepository.findAll(spec, sort).stream()
            .map(publicBlogService::toDetailDTO)
            .collect(Collectors.toList());
    }

    // ========================
    // PARK FEES
    // ========================

    /**
     * Conservation fees for a non-resident adult, every park, every season.
     *
     * <p>Filtered in memory rather than in SQL because the whole rate table is a few thousand
     * rows and this runs once a day behind a cache; a hand-written join across four lazy
     * associations would be the more fragile of the two.
     *
     * <p>The amount published is the <b>STO</b> rate. See {@link PublicParkFeeDTO} - for a
     * government conservation fee that is the real gate price, and rack would overstate it by
     * this company's own margin.
     */
    private List<PublicParkFeeDTO> parkFees() {
        return parkTariffRateRepository.findAll().stream()
            .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
            .filter(r -> r.getParkTariff() != null
                && r.getParkTariff().getTariff() != null
                && CONSERVATION_FEE.equalsIgnoreCase(r.getParkTariff().getTariff().getName()))
            .filter(r -> r.getNationCategory() != null
                && NON_RESIDENT.equalsIgnoreCase(r.getNationCategory().getName()))
            .filter(r -> r.getAgeCategory() != null
                && ADULT.equalsIgnoreCase(r.getAgeCategory().getName()))
            /* A park that is not on the website has no business publishing its gate price. */
            .filter(r -> {
                Park park = r.getParkTariff().getPark();
                return park != null
                    && Boolean.TRUE.equals(park.getIsActive())
                    && Boolean.TRUE.equals(park.getIsWebActive())
                    && park.getSlug() != null;
            })
            /* No STO rate means nobody has recorded the gate price; say nothing rather than
             * publish the sell price in its place. */
            .filter(r -> r.getStoRate() != null)
            .map(this::toFeeDTO)
            .sorted(Comparator.comparing(PublicParkFeeDTO::getParkName, Comparator.nullsLast(String::compareTo))
                .thenComparing(PublicParkFeeDTO::getSeason, Comparator.nullsLast(String::compareTo)))
            .collect(Collectors.toList());
    }

    private PublicParkFeeDTO toFeeDTO(ParkTariffRate r) {
        Park park = r.getParkTariff().getPark();
        return PublicParkFeeDTO.builder()
            .parkSlug(park.getSlug())
            .parkName(park.getName())
            .tariff(r.getParkTariff().getTariff().getName())
            .chargingBasis(basisOf(r))
            .nationCategory(r.getNationCategory().getName())
            .ageCategory(r.getAgeCategory().getName())
            .season(r.getSeason() != null ? r.getSeason().getName() : null)
            .amount(r.getStoRate())
            .currency(r.getCurrency())
            .build();
    }

    /** "Per Person" rather than PER_PERSON: this is read by a person, not matched by code. */
    private String basisOf(ParkTariffRate r) {
        return r.getParkTariff().getTariff().getChargingBasisDisplay();
    }
}

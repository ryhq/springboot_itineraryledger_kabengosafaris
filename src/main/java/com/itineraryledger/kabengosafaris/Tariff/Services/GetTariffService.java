package com.itineraryledger.kabengosafaris.Tariff.Services;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Tariff.DTOs.TariffDTO;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;
import com.itineraryledger.kabengosafaris.Tariff.Specifications.TariffSpecification;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * GetTariffService - Service for retrieving tariffs
 *
 * Provides methods for:
 * - Get single tariff by ID
 * - Get all tariffs with pagination and filtering
 * - Get tariffs by specific criteria
 */
@Service
@Slf4j
public class GetTariffService {

    private final TariffRepository tariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public GetTariffService(
        TariffRepository tariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.tariffRepository = tariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single tariff by ID
     *
     * @param id The obfuscated tariff ID
     * @return ResponseEntity with ApiResponse containing the tariff
     */
    public ResponseEntity<ApiResponse<?>> getTariffById(String id) {
        log.info("Fetching tariff by ID: {}", id);

        try {
            // Decode ID
            Long decodedId = idObfuscator.decodeId(id);
            if (decodedId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid tariff ID", "INVALID_TARIFF_ID")
                );
            }

            // Find tariff
            Optional<Tariff> tariffOpt = tariffRepository.findById(decodedId);
            if (tariffOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Tariff not found", "TARIFF_NOT_FOUND")
                );
            }

            Tariff tariff = tariffOpt.get();
            TariffDTO tariffDTO = convertToDTO(tariff);

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Tariff retrieved successfully",
                    tariffDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching tariff by ID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch tariff: " + e.getMessage(),
                    "TARIFF_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all tariffs with pagination, sorting, and filtering
     */
    public ResponseEntity<ApiResponse<?>> getAllTariffs(
        String name,
        String slug,
        ChargingBasis chargingBasis,
        Boolean isActive,
        Boolean isSystem,
        String keyword,
        Integer page,
        Integer size,
        String sortDirection
    ) {
        log.info("Fetching all tariffs with filters");

        try {
            // Build specification
            Specification<Tariff> spec = Specification.unrestricted();

            if (name != null && !name.trim().isEmpty()) {
                spec = spec.and(TariffSpecification.nameLike(name));
            }
            if (slug != null && !slug.trim().isEmpty()) {
                spec = spec.and(TariffSpecification.hasSlugLike(slug));
            }
            if (chargingBasis != null) {
                spec = spec.and(TariffSpecification.hasChargingBasis(chargingBasis));
            }
            if (isActive != null) {
                spec = spec.and(TariffSpecification.isActive(isActive));
            }
            if (isSystem != null) {
                spec = spec.and(TariffSpecification.isSystem(isSystem));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                spec = spec.and(TariffSpecification.searchKeyword(keyword));
            }

            // Build sort - default to createdAt descending
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
            Sort sort = Sort.by(direction, "createdAt");

            // Build pageable
            Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10,
                sort
            );

            // Execute query
            Page<Tariff> tariffPage = tariffRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<TariffDTO> tariffDTOs = tariffPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Build response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("tariffs", tariffDTOs);
            responseData.put("currentPage", tariffPage.getNumber());
            responseData.put("totalItems", tariffPage.getTotalElements());
            responseData.put("totalPages", tariffPage.getTotalPages());

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Tariffs retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch tariffs: " + e.getMessage(),
                    "TARIFFS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get tariff by slug
     *
     * @param slug The tariff slug
     * @return ResponseEntity with ApiResponse containing the tariff
     */
    public ResponseEntity<ApiResponse<?>> getTariffBySlug(String slug) {
        log.info("Fetching tariff by slug: {}", slug);

        try {
            if (slug == null || slug.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Slug is required", "INVALID_SLUG")
                );
            }

            Optional<Tariff> tariffOpt = tariffRepository.findBySlug(slug.trim());
            if (tariffOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Tariff not found", "TARIFF_NOT_FOUND")
                );
            }

            TariffDTO tariffDTO = convertToDTO(tariffOpt.get());

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Tariff retrieved successfully",
                    tariffDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching tariff by slug", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch tariff: " + e.getMessage(),
                    "TARIFF_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert Tariff entity to DTO
     */
    private TariffDTO convertToDTO(Tariff tariff) {
        return TariffDTO.builder()
            .id(idObfuscator.encodeId(tariff.getId()))
            .name(tariff.getName())
            .slug(tariff.getSlug())
            .chargingBasis(tariff.getChargingBasis())
            .chargingBasisDisplayName(tariff.getChargingBasisDisplay())
            .description(tariff.getDescription())
            .requiresAgeCategory(tariff.requiresAgeCategory())
            .isActive(tariff.getIsActive())
            .isSystem(tariff.getIsSystem())
            .createdAt(tariff.getCreatedAt())
            .updatedAt(tariff.getUpdatedAt())
            .build();
    }
}

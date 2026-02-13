package com.itineraryledger.kabengosafaris.Hero.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;

@Repository
public interface HeroImageRepository extends JpaRepository<HeroImage, Long>, JpaSpecificationExecutor<HeroImage> {

    /**
     * Find all images for a specific hero
     */
    List<HeroImage> findByHeroId(Long heroId);

    /**
     * Find all active images for a specific hero, ordered by display order
     */
    List<HeroImage> findByHeroIdAndIsActiveTrueOrderByDisplayOrderAsc(Long heroId);

    /**
     * Find primary image for a specific hero
     */
    HeroImage findByHeroIdAndIsPrimaryTrue(Long heroId);

    /**
     * Count images for a specific hero
     */
    long countByHeroId(Long heroId);

    /**
     * Delete all images for a specific hero
     */
    void deleteByHeroId(Long heroId);

    /**
     * Find image by filename
     */
    Optional<HeroImage> findByFileName(String fileName);

    /**
     * Check if filename exists
     */
    boolean existsByFileName(String fileName);

    /**
     * Unset primary flag for all images of a hero
     */
    @Modifying
    @Query("UPDATE HeroImage img SET img.isPrimary = false WHERE img.hero.id = :heroId")
    void unsetPrimaryForHero(@Param("heroId") Long heroId);

    /**
     * Get max display order for a hero
     */
    @Query("SELECT COALESCE(MAX(img.displayOrder), 0) FROM HeroImage img WHERE img.hero.id = :heroId")
    Integer findMaxDisplayOrderByHeroId(@Param("heroId") Long heroId);

    /**
     * Find images with pagination
     */
    @Query("SELECT img FROM HeroImage img WHERE img.hero.id = :heroId")
    Page<HeroImage> findByHeroIdPaginated(@Param("heroId") Long heroId, Pageable pageable);
}

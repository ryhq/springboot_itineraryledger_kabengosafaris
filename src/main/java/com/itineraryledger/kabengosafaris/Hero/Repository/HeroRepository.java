package com.itineraryledger.kabengosafaris.Hero.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

@Repository
public interface HeroRepository extends JpaRepository<Hero, Long>, JpaSpecificationExecutor<Hero> {

    /**
     * Find all heroes for a specific page
     */
    List<Hero> findByPage(HeroPage page);

    /**
     * Find all active heroes for a specific page, ordered by display order
     */
    List<Hero> findByPageAndIsActiveTrueOrderByDisplayOrderAsc(HeroPage page);

    /**
     * Find all heroes for a specific page, ordered by display order
     */
    List<Hero> findByPageOrderByDisplayOrderAsc(HeroPage page);

    /**
     * Count heroes for a specific page
     */
    long countByPage(HeroPage page);

    /**
     * Count active heroes for a specific page
     */
    long countByPageAndIsActiveTrue(HeroPage page);
}

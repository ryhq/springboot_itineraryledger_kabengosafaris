package com.itineraryledger.kabengosafaris.Faq.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Faq.Entity.Faq;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long>, JpaSpecificationExecutor<Faq> {

    List<Faq> findByIsActiveTrueOrderByDisplayOrderAscIdAsc();

    boolean existsByQuestion(String question);

    @Query("SELECT COALESCE(MAX(f.displayOrder), 0) FROM Faq f")
    Integer findMaxDisplayOrder();
}

package com.itineraryledger.kabengosafaris.Testimony.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;

@Repository
public interface TestimonyImageRepository extends JpaRepository<TestimonyImage, Long>, JpaSpecificationExecutor<TestimonyImage> {

    List<TestimonyImage> findByTestimonyId(Long testimonyId);

    List<TestimonyImage> findByTestimonyIdAndIsActiveTrueOrderByDisplayOrderAsc(Long testimonyId);

    Optional<TestimonyImage> findByTestimonyIdAndIsPrimaryTrue(Long testimonyId);

    long countByTestimonyId(Long testimonyId);

    Optional<TestimonyImage> findByFileName(String fileName);

    boolean existsByFileName(String fileName);

    @Modifying
    @Query("UPDATE TestimonyImage img SET img.isPrimary = false WHERE img.testimony.id = :testimonyId")
    void unsetPrimaryForTestimony(@Param("testimonyId") Long testimonyId);

    @Query("SELECT COALESCE(MAX(img.displayOrder), 0) FROM TestimonyImage img WHERE img.testimony.id = :testimonyId")
    Integer findMaxDisplayOrderByTestimonyId(@Param("testimonyId") Long testimonyId);

    @Query("SELECT i.id FROM TestimonyImage i WHERE i.id > :currentId ORDER BY i.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT i.id FROM TestimonyImage i WHERE i.id < :currentId ORDER BY i.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT i.id FROM TestimonyImage i ORDER BY i.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT i.id FROM TestimonyImage i ORDER BY i.id DESC LIMIT 1")
    Optional<Long> findLastId();
}

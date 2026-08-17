package com.itineraryledger.kabengosafaris.Blog.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long>, JpaSpecificationExecutor<Blog> {

    Optional<Blog> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** The public list: published only, newest first, with the editorial order first. */
    List<Blog> findByIsPublishedTrueOrderByDisplayOrderAscPublishDateDesc();

    Optional<Blog> findBySlugAndIsPublishedTrue(String slug);

    @Query("SELECT COALESCE(MAX(b.displayOrder), 0) FROM Blog b")
    Integer findMaxDisplayOrder();
}

package com.itineraryledger.kabengosafaris.Blog.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;

@Repository
public interface BlogImageRepository extends JpaRepository<BlogImage, Long>, JpaSpecificationExecutor<BlogImage> {

    List<BlogImage> findByBlogIdOrderByDisplayOrderAscIdAsc(Long blogId);

    Optional<BlogImage> findByFileName(String fileName);

    long countByBlogId(Long blogId);

    Optional<BlogImage> findFirstByBlogIdAndIsPrimaryTrue(Long blogId);

    @Query("SELECT COALESCE(MAX(i.displayOrder), 0) FROM BlogImage i WHERE i.blog.id = :blogId")
    Integer findMaxDisplayOrderInBlog(@Param("blogId") Long blogId);

    @Query("UPDATE BlogImage i SET i.isPrimary = false WHERE i.blog.id = :blogId AND i.id <> :keepId")
    @org.springframework.data.jpa.repository.Modifying
    void clearPrimaryExcept(@Param("blogId") Long blogId, @Param("keepId") Long keepId);
}

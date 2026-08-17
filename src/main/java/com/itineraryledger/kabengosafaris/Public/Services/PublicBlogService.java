package com.itineraryledger.kabengosafaris.Public.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogBlockDTO;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogFaqDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;
import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageStorageService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogServices.BlogContentService;
import com.itineraryledger.kabengosafaris.Blog.Specifications.BlogSpecification;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicBlogBlockDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicBlogDetailDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicBlogFaqDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicBlogImageDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicBlogListDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The blog, as the public website reads it.
 *
 * Published posts only, and never anything internal: no ids, no draft flags, no author
 * accounts, no display order. The JSON matches the website's own BlogPost interface, so
 * wiring the Next.js pages is a change of data source rather than a rewrite.
 *
 * English is the source; every reader-facing string is @Translatable and localised by the
 * translation service when a locale is asked for.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PublicBlogService {

    private final BlogRepository blogRepository;
    private final BlogContentService contentService;
    private final BlogImageStorageService imageStorageService;
    private final PublicTranslationService publicTranslationService;
    private final IdObfuscator idObfuscator;

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getPublicBlogs(Integer page, Integer size, String lang) {
        try {
            /* published only, and in the editorial order the panel set */
            Specification<Blog> spec = Specification.<Blog>unrestricted()
                .and(BlogSpecification.isPublished(true));

            Sort sort = Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("publishDate"));

            if (page == null && size == null) {
                List<PublicBlogListDTO> dtos = blogRepository.findAll(spec, sort).stream()
                    .map(this::toListDTO)
                    .collect(Collectors.toList());
                publicTranslationService.translateDtoList(dtos, lang);
                return ResponseEntity.ok(ApiResponse.success(200, "Blogs retrieved successfully", dtos));
            }

            int pageNumber = page != null && page >= 0 ? page : 0;
            int pageSize = size != null && size > 0 ? Math.min(size, 50) : 9;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
            Page<Blog> blogPage = blogRepository.findAll(spec, pageable);

            List<PublicBlogListDTO> dtos = blogPage.getContent().stream()
                .map(this::toListDTO)
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(dtos, lang);

            Map<String, Object> response = new HashMap<>();
            response.put("blogs", dtos);
            response.put("currentPage", blogPage.getNumber());
            response.put("totalItems", blogPage.getTotalElements());
            response.put("totalPages", blogPage.getTotalPages());
            response.put("pageSize", blogPage.getSize());

            return ResponseEntity.ok(ApiResponse.success(200, "Blogs retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching public blogs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch blogs", "BLOGS_FETCH_FAILED"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getPublicBlogBySlug(String slug, String lang) {
        try {
            Blog blog = blogRepository.findBySlugAndIsPublishedTrue(slug).orElse(null);
            if (blog == null) {
                /*
                 * 404 whether it is unpublished or absent: telling the internet that a draft
                 * exists at this address is a leak, however small.
                 */
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "Blog post not found", "BLOG_NOT_FOUND"));
            }

            PublicBlogDetailDTO dto = toDetailDTO(blog);
            publicTranslationService.translateDto(dto, lang);
            return ResponseEntity.ok(ApiResponse.success(200, "Blog post retrieved successfully", dto));
        } catch (Exception e) {
            log.error("Error fetching public blog: {}", slug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch blog post", "BLOG_FETCH_FAILED"));
        }
    }

    private PublicBlogListDTO toListDTO(Blog blog) {
        return PublicBlogListDTO.builder()
            .slug(blog.getSlug())
            .title(blog.getTitle())
            .excerpt(blog.getExcerpt())
            .date(blog.getPublishDate())
            .author(blog.getAuthor())
            .readMinutes(readMinutesOf(blog))
            .tags(blog.getTags() == null ? List.of() : new ArrayList<>(blog.getTags()))
            .coverImageUrl(coverUrl(blog))
            .build();
    }

    private PublicBlogDetailDTO toDetailDTO(Blog blog) {
        List<BlogBlockDTO> blocks = contentService.readBlocks(blog.getBodyJson());
        List<BlogFaqDTO> faqs = contentService.readFaqs(blog.getFaqsJson());

        return PublicBlogDetailDTO.builder()
            .slug(blog.getSlug())
            .title(blog.getTitle())
            .excerpt(blog.getExcerpt())
            .date(blog.getPublishDate())
            .author(blog.getAuthor())
            .readMinutes(readMinutesOf(blog))
            .tags(blog.getTags() == null ? List.of() : new ArrayList<>(blog.getTags()))
            .body(blocks.stream().map(this::toPublicBlock).collect(Collectors.toList()))
            .faqs(faqs.stream()
                .map(faq -> PublicBlogFaqDTO.builder().q(faq.getQ()).a(faq.getA()).build())
                .collect(Collectors.toList()))
            .coverImageUrl(coverUrl(blog))
            .images(activeImages(blog))
            .metaTitle(blog.getMetaTitle())
            .metaDescription(blog.getMetaDescription())
            .build();
    }

    private PublicBlogBlockDTO toPublicBlock(BlogBlockDTO block) {
        return PublicBlogBlockDTO.builder()
            .type(block.getType())
            .text(block.getText())
            .items(block.getItems() == null ? null : new ArrayList<>(block.getItems()))
            .url(block.getUrl())
            .alt(block.getAlt())
            .caption(block.getCaption())
            .build();
    }

    private List<PublicBlogImageDTO> activeImages(Blog blog) {
        if (blog.getImages() == null) return List.of();
        return blog.getImages().stream()
            .filter(image -> Boolean.TRUE.equals(image.getIsActive()))
            .map(image -> PublicBlogImageDTO.builder()
                .url(imageStorageService.constructImageUrl(idObfuscator.encodeId(image.getId())))
                .alt(image.getAltText())
                .caption(image.getCaption())
                .isCover(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .width(image.getWidth())
                .height(image.getHeight())
                .build())
            .collect(Collectors.toList());
    }

    private String coverUrl(Blog blog) {
        if (blog.getImages() == null || blog.getImages().isEmpty()) return null;
        BlogImage cover = blog.getImages().stream()
            .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()) && Boolean.TRUE.equals(image.getIsActive()))
            .findFirst()
            .orElseGet(() -> blog.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsActive()))
                .findFirst()
                .orElse(null));
        return cover == null ? null : imageStorageService.constructImageUrl(idObfuscator.encodeId(cover.getId()));
    }

    /** Never null on the public side: the site prints "N min read" and would print "null". */
    private Integer readMinutesOf(Blog blog) {
        if (blog.getReadMinutes() != null && blog.getReadMinutes() > 0) return blog.getReadMinutes();
        return contentService.estimateReadMinutes(contentService.readBlocks(blog.getBodyJson()));
    }
}

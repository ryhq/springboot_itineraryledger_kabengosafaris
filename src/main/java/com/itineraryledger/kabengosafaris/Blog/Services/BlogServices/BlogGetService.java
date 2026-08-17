package com.itineraryledger.kabengosafaris.Blog.Services.BlogServices;

import java.util.Arrays;
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
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogDTO;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogListItemDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;
import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageGetService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageStorageService;
import com.itineraryledger.kabengosafaris.Blog.Specifications.BlogFilter;
import com.itineraryledger.kabengosafaris.Blog.Specifications.BlogSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reading blog posts.
 *
 * The list carries no body — an article is thousands of words and twenty of them would be
 * megabytes for a table showing titles. The record carries everything, including the parsed
 * block list, so the editor never has to parse JSON itself.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogGetService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "title", "slug", "author", "publishDate", "readMinutes", "isPublished", "displayOrder", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "publishDate";

    private final BlogRepository blogRepository;
    private final BlogContentService contentService;
    private final BlogImageGetService imageGetService;
    private final BlogImageStorageService imageStorageService;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    /**
     * The ONE description of the filtered set, shared by the rows, the counters and the
     * record arrows — so a card and the table under it cannot disagree.
     */
    private Specification<Blog> buildSpec(BlogFilter filter) {
        Specification<Blog> spec = Specification.unrestricted();
        if (filter == null) return spec;

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(BlogSpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getAuthor() != null && !filter.getAuthor().isBlank()) {
            spec = spec.and(BlogSpecification.authorLike(filter.getAuthor()));
        }
        if (filter.getTag() != null && !filter.getTag().isBlank()) {
            spec = spec.and(BlogSpecification.hasTag(filter.getTag()));
        }
        if (filter.getIsPublished() != null) {
            spec = spec.and(BlogSpecification.isPublished(filter.getIsPublished()));
        }
        /* published + draft together is every row, so the pair cancels to no constraint */
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            boolean published = filter.getStatuses().contains("published");
            boolean draft = filter.getStatuses().contains("draft");
            if (published != draft) spec = spec.and(BlogSpecification.isPublished(published));
        }
        if (filter.getQualities() != null && !filter.getQualities().isEmpty()) {
            Specification<Blog> quality = null;
            for (String want : filter.getQualities()) {
                Specification<Blog> one = switch (want) {
                    case "no-excerpt" -> BlogSpecification.missingExcerpt();
                    case "no-cover" -> BlogSpecification.missingCover();
                    case "no-meta" -> BlogSpecification.missingMeta();
                    case "empty-body" -> BlogSpecification.emptyBody();
                    default -> null;
                };
                if (one == null) continue;
                quality = quality == null ? one : quality.or(one);
            }
            if (quality != null) spec = spec.and(quality);
        }
        if (filter.getPublishedAfter() != null) {
            spec = spec.and(BlogSpecification.publishedAfter(filter.getPublishedAfter()));
        }
        if (filter.getPublishedBefore() != null) {
            spec = spec.and(BlogSpecification.publishedBefore(filter.getPublishedBefore()));
        }
        return spec;
    }

    public ResponseEntity<ApiResponse<?>> getAllBlogs(
        BlogFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            int pageNumber = page != null && page >= 0 ? page : 0;
            int pageSize = size != null && size > 0 ? Math.min(size, 100) : 20;

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            Specification<Blog> spec = buildSpec(filter);
            Page<Blog> blogPage = blogRepository.findAll(spec, pageable);

            List<BlogListItemDTO> dtos = blogPage.getContent().stream()
                .map(this::toListItemDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("blogs", dtos);
            response.put("currentPage", blogPage.getNumber());
            response.put("totalItems", blogPage.getTotalElements());
            response.put("totalPages", blogPage.getTotalPages());
            response.put("pageSize", blogPage.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());

            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Blogs retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing blogs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list blogs", "BLOGS_LIST_FAILED")
            );
        }
    }

    /**
     * Counters for the WHOLE filtered set.
     *
     * Every one of these is reachable as a filter, and every filter has one — a figure you
     * cannot click is decoration.
     */
    private Map<String, Object> buildStats(Specification<Blog> spec) {
        return listStats.of(Blog.class, spec)
            .total()
            .count("published", BlogSpecification.isPublished(true))
            .complement("draft", "published")
            .count("missingExcerpt", BlogSpecification.missingExcerpt())
            .count("missingCover", BlogSpecification.missingCover())
            .count("missingMeta", BlogSpecification.missingMeta())
            .count("emptyBody", BlogSpecification.emptyBody())
            .build();
    }

    public ResponseEntity<ApiResponse<?>> getBlogById(
        String obfuscatedId,
        BlogFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid blog ID", "INVALID_BLOG_ID")
                );
            }

            Blog blog = blogRepository.findById(id).orElse(null);
            if (blog == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Blog post not found", "BLOG_NOT_FOUND")
                );
            }

            /* the arrows walk the SAME filtered, sorted set the list was showing */
            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                Blog.class,
                buildSpec(filter),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                "asc".equalsIgnoreCase(sortDirection),
                id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("blog", toDTO(blog));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Blog retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching blog", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch blog", "BLOG_FETCH_FAILED")
            );
        }
    }

    /** For the editor's "is this slug free?" check and for the public reader. */
    public Blog findBySlug(String slug) {
        return slug == null ? null : blogRepository.findBySlug(slug).orElse(null);
    }

    public BlogDTO toDTO(Blog blog) {
        if (blog == null) return null;
        List<BlogBlockDTO> blocks = contentService.readBlocks(blog.getBodyJson());
        BlogImage cover = coverOf(blog);

        return BlogDTO.builder()
            .id(idObfuscator.encodeId(blog.getId()))
            .slug(blog.getSlug())
            .title(blog.getTitle())
            .excerpt(blog.getExcerpt())
            .author(blog.getAuthor())
            .publishDate(blog.getPublishDate())
            .readMinutes(blog.getReadMinutes())
            .tags(blog.getTags())
            .body(blocks)
            .faqs(contentService.readFaqs(blog.getFaqsJson()))
            .isPublished(blog.getIsPublished())
            .firstPublishedAt(blog.getFirstPublishedAt())
            .displayOrder(blog.getDisplayOrder())
            .metaTitle(blog.getMetaTitle())
            .metaDescription(blog.getMetaDescription())
            .images(blog.getImages() == null ? List.of() : blog.getImages().stream()
                .map(imageGetService::toDTO)
                .collect(Collectors.toList()))
            .coverImageUrl(cover == null ? null : imageStorageService.constructImageUrl(idObfuscator.encodeId(cover.getId())))
            .imageCount(blog.getImages() == null ? 0L : (long) blog.getImages().size())
            .wordCount(contentService.wordCount(blocks))
            .createdByName(blog.getCreatedBy() != null ? blog.getCreatedBy().getUsername() : null)
            .updatedByName(blog.getUpdatedBy() != null ? blog.getUpdatedBy().getUsername() : null)
            .createdAt(blog.getCreatedAt())
            .updatedAt(blog.getUpdatedAt())
            .build();
    }

    public BlogListItemDTO toListItemDTO(Blog blog) {
        if (blog == null) return null;
        BlogImage cover = coverOf(blog);
        List<BlogBlockDTO> blocks = contentService.readBlocks(blog.getBodyJson());
        return BlogListItemDTO.builder()
            .id(idObfuscator.encodeId(blog.getId()))
            .slug(blog.getSlug())
            .title(blog.getTitle())
            .excerpt(blog.getExcerpt())
            .author(blog.getAuthor())
            .publishDate(blog.getPublishDate())
            .readMinutes(blog.getReadMinutes())
            .tags(blog.getTags())
            .isPublished(blog.getIsPublished())
            .firstPublishedAt(blog.getFirstPublishedAt())
            .displayOrder(blog.getDisplayOrder())
            .coverImageUrl(cover == null ? null : imageStorageService.constructImageUrl(idObfuscator.encodeId(cover.getId())))
            .imageCount(blog.getImages() == null ? 0L : (long) blog.getImages().size())
            .blockCount(blocks.size())
            .faqCount(contentService.readFaqs(blog.getFaqsJson()).size())
            .hasCover(cover != null)
            .createdAt(blog.getCreatedAt())
            .updatedAt(blog.getUpdatedAt())
            .build();
    }

    /** The cover, or the first image if nothing was ever marked — never nothing at all. */
    public BlogImage coverOf(Blog blog) {
        if (blog == null || blog.getImages() == null || blog.getImages().isEmpty()) return null;
        return blog.getImages().stream()
            .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()) && Boolean.TRUE.equals(image.getIsActive()))
            .findFirst()
            .orElseGet(() -> blog.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsActive()))
                .findFirst()
                .orElse(null));
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }
}

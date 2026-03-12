package com.itineraryledger.kabengosafaris.Public.Services;

import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PublicServiceUtils {

    private PublicServiceUtils() {}

    public static <T> Map<String, Object> buildPageResponse(String key, List<?> items, Page<T> page) {
        Map<String, Object> response = new HashMap<>();
        response.put(key, items);
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("pageSize", page.getSize());
        return response;
    }
}

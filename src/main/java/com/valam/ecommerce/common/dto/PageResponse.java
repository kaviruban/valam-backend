package com.valam.ecommerce.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable JSON shape for paginated results; Spring Data's Page should not be serialized directly. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}

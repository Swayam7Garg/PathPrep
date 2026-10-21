package com.swayam.pathprep.dto.response;

import java.util.List;

/**
 * Generic pagination wrapper enclosing paginated list items alongside total counts, pages, and navigation metadata.
 */
public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long total,
        int totalPages
) {}

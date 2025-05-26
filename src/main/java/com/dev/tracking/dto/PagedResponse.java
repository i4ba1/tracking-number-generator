package com.dev.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for paginated operations
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedResponse {
    private List<TrackingNumberInfo> data;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private Instant retrievedAt;
    private boolean syncedWithDatabase;

    public PagedResponse(List<TrackingNumberInfo> data, int currentPage, int pageSize, long totalElements) {
        this.data = data;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        this.hasNext = currentPage < totalPages - 1;
        this.hasPrevious = currentPage > 0;
        this.retrievedAt = Instant.now();
        this.syncedWithDatabase = true;
    }
}
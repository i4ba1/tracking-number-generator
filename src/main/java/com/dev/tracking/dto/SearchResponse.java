package com.dev.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for search operations
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponse {
    private List<TrackingNumberInfo> results;
    private int totalFound;
    private String message;
    private Instant searchedAt;
    private String source; // "redis", "mongodb", or "both"

    public SearchResponse(List<TrackingNumberInfo> results, int totalFound, String source) {
        this.results = results;
        this.totalFound = totalFound;
        this.message = totalFound > 0 ? "Results found" : "No results found";
        this.searchedAt = Instant.now();
        this.source = source;
    }

    public static SearchResponse notFound() {
        return new SearchResponse(List.of(), 0, "No results found", Instant.now(), "both");
    }
}
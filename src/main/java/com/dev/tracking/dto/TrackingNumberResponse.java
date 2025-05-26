package com.dev.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for the tracking number generation response.
 *
 * This class defines what our API returns to clients. We keep it simple
 * but extensible, following the API specification while allowing for
 * future enhancements without breaking changes.
 *
 * The response format is designed to be both human-readable and
 * machine-parseable, supporting integration with various client systems.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingNumberResponse {

    /**
     * The generated tracking number that matches the required regex pattern.
     * This is the primary value that clients need for their business operations.
     */
    private String trackingNumber;

    /**
     * RFC 3339 timestamp indicating when this tracking number was generated.
     * This supports audit trails and helps clients understand data freshness.
     */
    private Instant createdAt;

    /**
     * Optional metadata that could be useful for debugging or analytics.
     * This demonstrates how we can extend the API response without breaking changes.
     */
    private String status = "success";

    public TrackingNumberResponse(String trackingNumber, Instant createdAt) {
        this.trackingNumber = trackingNumber;
        this.createdAt = createdAt;
    }
}

package com.dev.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Simplified tracking number information for responses
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackingNumberInfo {
    private String trackingNumber;
    private Instant createdAt;
    private String originCountryId;
    private String destinationCountryId;
    private Double weight;
    private String customerId;
    private String customerName;
    private String customerSlug;

    public TrackingNumberInfo(com.dev.tracking.model.TrackingNumber entity) {
        this.trackingNumber = entity.getTrackingNumber();
        this.createdAt = entity.getCreatedAt();
        this.originCountryId = entity.getOriginCountryId();
        this.destinationCountryId = entity.getDestinationCountryId();
        this.weight = entity.getWeight();
        this.customerId = entity.getCustomerId();
        this.customerName = entity.getCustomerName();
        this.customerSlug = entity.getCustomerSlug();
    }
}

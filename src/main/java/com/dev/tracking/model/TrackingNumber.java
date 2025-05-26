package com.dev.tracking.model;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Entity representing a generated tracking number in our system.
 * <p>
 * This class models the core business object that we store in MongoDB.
 * We use MongoDB's document-based storage because it provides excellent
 * horizontal scaling capabilities and flexible schema evolution.
 * <p>
 * Key design decisions:
 * - The tracking number itself is indexed for fast lookups and uniqueness validation
 * - We store creation metadata to support auditing and debugging
 * - Customer information is denormalized for query performance
 * - Weight and route information support future analytics requirements
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "tracking_numbers")
public class TrackingNumber {

    @Id
    private String id;

    /**
     * The generated tracking number - our primary business identifier.
     * This field is indexed to ensure uniqueness and enable fast lookups.
     */
    @Indexed(unique = true)
    private String trackingNumber;

    /**
     * Timestamp when this tracking number was generated.
     * Uses Spring Data's @CreatedDate for automatic population.
     */
    @CreatedDate
    private Instant createdAt;

    // Order information - stored for potential future analytics and reporting
    private String originCountryId;
    private String destinationCountryId;
    private Double weight;
    private Instant orderCreatedAt;

    // Customer information - denormalized for performance
    private String customerId;
    private String customerName;
    private String customerSlug;


    public TrackingNumber(String trackingNumber,
                          @NotBlank(message = "Origin country ID is required") @Pattern(regexp = "^[A-Z]{2}$", message = "Origin country ID must be a 2-letter ISO 3166-1 alpha-2 code") String originCountryId,
                          @NotBlank(message = "Destination country ID is required") @Pattern(regexp = "^[A-Z]{2}$", message = "Destination country ID must be a 2-letter ISO 3166-1 alpha-2 code") String destinationCountryId,
                          @NotNull(message = "Weight is required") @DecimalMin(value = "0.001", message = "Weight must be greater than 0") @DecimalMax(value = "999.999", message = "Weight must not exceed 999.999 kg") Double weight,
                          @NotNull(message = "Created at timestamp is required") Instant createdAt,
                          @NotBlank(message = "Customer ID is required") @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                                  message = "Customer ID must be a valid UUID") String customerId,
                          @NotBlank(message = "Customer name is required") @Size(min = 1, max = 100, message = "Customer name must be between 1 and 100 characters") String customerName,
                          @NotBlank(message = "Customer slug is required") @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                                  message = "Customer slug must be in kebab-case format (lowercase letters, numbers, and hyphens)") String customerSlug) {
        this.trackingNumber = trackingNumber;
        this.originCountryId = originCountryId;
        this.destinationCountryId = destinationCountryId;
        this.weight = weight;
        this.createdAt = createdAt;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerSlug = customerSlug;
        this.orderCreatedAt = createdAt;
    }
}

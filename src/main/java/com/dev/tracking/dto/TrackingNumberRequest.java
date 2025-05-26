package com.dev.tracking.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingNumberRequest {

    /**
     * Origin country code in ISO 3166-1 alpha-2 format.
     * We validate this to ensure it's exactly 2 uppercase letters.
     */
    @NotBlank(message = "Origin country ID is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Origin country ID must be a 2-letter ISO 3166-1 alpha-2 code")
    private String originCountryId;

    /**
     * Destination country code in ISO 3166-1 alpha-2 format.
     * Same validation as origin country for consistency.
     */
    @NotBlank(message = "Destination country ID is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Destination country ID must be a 2-letter ISO 3166-1 alpha-2 code")
    private String destinationCountryId;

    /**
     * Package weight in kilograms with up to 3 decimal places.
     * We use DecimalMax/Min to enforce reasonable business constraints.
     */
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.001", message = "Weight must be greater than 0")
    @DecimalMax(value = "999.999", message = "Weight must not exceed 999.999 kg")
    private Double weight;

    /**
     * Order creation timestamp in RFC 3339 format.
     * This helps us maintain audit trails and support time-based analytics.
     */
    @NotNull(message = "Created at timestamp is required")
    private Instant createdAt;

    /**
     * Customer UUID - we validate the format to ensure data quality.
     * UUIDs provide globally unique customer identification.
     */
    @NotBlank(message = "Customer ID is required")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            message = "Customer ID must be a valid UUID")
    private String customerId;

    /**
     * Human-readable customer name for display purposes.
     */
    @NotBlank(message = "Customer name is required")
    @Size(min = 1, max = 100, message = "Customer name must be between 1 and 100 characters")
    private String customerName;

    /**
     * URL-friendly customer identifier in kebab-case format.
     * This supports SEO-friendly URLs and consistent naming conventions.
     */
    @NotBlank(message = "Customer slug is required")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "Customer slug must be in kebab-case format (lowercase letters, numbers, and hyphens)")
    private String customerSlug;

}

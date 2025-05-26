package com.dev.tracking.controller;

import com.dev.tracking.dto.TrackingNumberRequest;
import com.dev.tracking.service.TrackingNumberService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * REST controller for handling tracking number generation requests.
 *
 * This controller serves as the entry point for our API, handling HTTP requests
 * and orchestrating the business logic. It's designed following RESTful principles
 * and implements proper error handling for a production-ready API.
 *
 * Key design principles implemented:
 * 1. Reactive programming for high concurrency
 * 2. Proper HTTP status codes and error responses
 * 3. Input validation with meaningful error messages
 * 4. Separation of concerns between web layer and business logic
 * 5. Comprehensive error handling for edge cases
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
public class TrackingNumberController {

    private final TrackingNumberService trackingNumberService;

    /**
     * Generates a new tracking number based on the provided parameters.
     *
     * This endpoint implements the core API specification:
     * - Accepts GET requests to /next-tracking-number
     * - Validates all required query parameters
     * - Returns JSON response with tracking number and creation timestamp
     * - Handles errors gracefully with appropriate HTTP status codes
     *
     * The method signature uses query parameters as specified in the requirements,
     * while internally converting them to a proper DTO for validation and processing.
     *
     * @param originCountryId Origin country in ISO 3166-1 alpha-2 format
     * @param destinationCountryId Destination country in ISO 3166-1 alpha-2 format
     * @param weight Package weight in kilograms (up to 3 decimal places)
     * @param createdAt Order creation timestamp in RFC 3339 format
     * @param customerId Customer UUID
     * @param customerName Customer display name
     * @param customerSlug Customer identifier in kebab-case
     * @return Reactive response containing the generated tracking number
     */
    @GetMapping("/next-tracking-number")
    public Mono<ResponseEntity<Object>> generateTrackingNumber(
            @RequestParam("origin_country_id") String originCountryId,
            @RequestParam("destination_country_id") String destinationCountryId,
            @RequestParam("weight") String weight,
            @RequestParam("created_at") String createdAt,
            @RequestParam("customer_id") String customerId,
            @RequestParam("customer_name") String customerName,
            @RequestParam("customer_slug") String customerSlug) {

        log.info("Received request to generate tracking number");
        return buildRequestDto(originCountryId, destinationCountryId, weight, createdAt,
                customerId, customerName, customerSlug)
                .flatMap(trackingNumberService::generateTrackingNumber)
                .map(response -> ResponseEntity.ok().body((Object) response))
                .onErrorResume(this::handleError);
    }

    /**
     * Builds and validates the request DTO from query parameters.
     *
     * This method centralizes the conversion from raw query parameters to
     * our validated DTO object. It handles type conversion (especially for
     * weight and timestamp) and provides meaningful error messages for
     * invalid input data.
     *
     * The reactive approach allows us to handle validation errors as part
     * of the same reactive stream, maintaining consistency in error handling.
     */
    private Mono<TrackingNumberRequest> buildRequestDto(String originCountryId,
                                                        String destinationCountryId,
                                                        String weight, String createdAt,
                                                        String customerId, String customerName,
                                                        String customerSlug) {
        return Mono.fromCallable(() -> {
            try {
                // Parse and validate weight
                Double weightValue = parseWeight(weight);

                // Parse and validate timestamp
                Instant createdAtValue = parseTimestamp(createdAt);

                // Create and return the request DTO
                return new TrackingNumberRequest(
                        originCountryId,
                        destinationCountryId,
                        weightValue,
                        createdAtValue,
                        customerId,
                        customerName,
                        customerSlug
                );
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid request parameters: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Parses and validates the weight parameter.
     *
     * We need to handle weight as a string initially because query parameters
     * are always strings. This method converts to Double while validating
     * the format and range constraints.
     */
    private Double parseWeight(String weight) {
        try {
            double value = Double.parseDouble(weight);

            // Validate range
            if (value <= 0 || value > 999.999) {
                throw new IllegalArgumentException("Weight must be between 0.001 and 999.999 kg");
            }

            // Validate decimal places (up to 3)
            String[] parts = weight.split("\\.");
            if (parts.length > 1 && parts[1].length() > 3) {
                throw new IllegalArgumentException("Weight can have at most 3 decimal places");
            }

            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Weight must be a valid number", e);
        }
    }

    /**
     * Parses and validates the timestamp parameter.
     *
     * The API specification requires RFC 3339 format, which is the standard
     * for ISO 8601 timestamps. Java's Instant.parse() handles this format
     * natively, but we provide better error messages for invalid input.
     */
    private Instant parseTimestamp(String createdAt) {
        try {
            return Instant.parse(createdAt);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Created at must be a valid RFC 3339 timestamp (e.g., '2018-11-20T19:29:32+08:00')", e);
        }
    }

    /**
     * Centralized error handling for the API endpoint.
     *
     * This method converts various types of exceptions into appropriate
     * HTTP responses with meaningful error messages. It follows REST API
     * best practices by using proper status codes and providing actionable
     * error information to clients.
     *
     * The error handling strategy is designed to be helpful for debugging
     * while not exposing internal system details that could be security risks.
     */
    private Mono<ResponseEntity<Object>> handleError(Throwable throwable) {
        if (throwable instanceof IllegalArgumentException) {
            // Client error - invalid input parameters
            return Mono.just(ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_PARAMETERS", throwable.getMessage())));
        } else if (throwable instanceof TrackingNumberService.TrackingNumberCollisionException) {
            // Rare case where our retry logic was exhausted
            return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("GENERATION_FAILED", "Unable to generate unique tracking number after multiple attempts")));
        } else {
            // Unexpected server error - log details but return generic message
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred while generating tracking number")));
        }
    }

    /**
     * Standardized error response format.
     *
     * This class provides a consistent structure for API error responses,
     * making it easier for clients to handle errors programmatically.
     * The format includes both a machine-readable error code and a
     * human-readable error message.
     */
    @Getter
    public static class ErrorResponse {
        private final String errorCode;
        private final String message;
        private final Instant timestamp;

        public ErrorResponse(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
            this.timestamp = Instant.now();
        }

    }
}
package com.dev.tracking.repository;


import com.dev.tracking.model.TrackingNumber;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository interface for tracking number data access operations.
 *
 * This interface extends ReactiveMongoRepository to provide non-blocking,
 * reactive data access operations. The reactive approach is crucial for
 * our scalability requirements because it allows a single thread to handle
 * many concurrent requests without blocking.
 *
 * Key benefits of this approach:
 * - Non-blocking I/O operations improve throughput under high load
 * - Reactive streams provide backpressure handling to prevent system overload
 * - MongoDB's native support for reactive operations ensures efficient database usage
 * - The repository pattern abstracts data access concerns from business logic
 *
 * Spring Data MongoDB automatically provides implementations for standard
 * CRUD operations, while we define custom methods for our specific business needs.
 */
@Repository
public interface TrackingNumberRepository extends ReactiveMongoRepository<TrackingNumber, String> {

    /**
     * Checks if a tracking number already exists in the database.
     *
     * This method is essential for ensuring uniqueness constraints.
     * We use this in our service layer to implement collision detection
     * and retry logic when generating tracking numbers.
     *
     * The reactive Mono<Boolean> return type allows this check to be
     * composed with other reactive operations without blocking threads.
     *
     * @param trackingNumber The tracking number to check for existence
     * @return A Mono that emits true if the tracking number exists, false otherwise
     */
    Mono<Boolean> existsByTrackingNumber(String trackingNumber);

    /**
     * Finds a tracking number entity by its tracking number value.
     *
     * This method supports lookup operations and can be used for
     * debugging or customer service operations. While not required
     * for the core API functionality, it demonstrates how we can
     * extend our data access layer to support additional use cases.
     *
     * @param trackingNumber The tracking number to look up
     * @return A Mono that emits the TrackingNumber entity if found, or empty if not found
     */
    Mono<TrackingNumber> findByTrackingNumber(String trackingNumber);
}

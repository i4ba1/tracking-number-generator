package com.dev.tracking.repository;


import com.dev.tracking.model.TrackingNumber;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TrackingNumberRepository extends ReactiveMongoRepository<TrackingNumber, String> {

    Mono<Boolean> existsByTrackingNumber(String trackingNumber);

    Mono<TrackingNumber> findByTrackingNumber(String trackingNumber);

    /**
     * Simplified search method - searches based on non-null parameters
     */
    default Flux<TrackingNumber> findBySearchCriteria(String trackingNumber, String customerName,
                                                      String customerSlug, String originCountryId,
                                                      String destinationCountryId) {
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            return findByTrackingNumber(trackingNumber).flux();
        }
        if (customerName != null && !customerName.trim().isEmpty()) {
            return findByCustomerNameContainingIgnoreCase(customerName);
        }
        if (customerSlug != null && !customerSlug.trim().isEmpty()) {
            return findByCustomerSlugContainingIgnoreCase(customerSlug);
        }
        if (originCountryId != null && !originCountryId.trim().isEmpty()) {
            if (destinationCountryId != null && !destinationCountryId.trim().isEmpty()) {
                return findByOriginCountryIdAndDestinationCountryId(originCountryId, destinationCountryId);
            }
            return findByOriginCountryId(originCountryId);
        }
        if (destinationCountryId != null && !destinationCountryId.trim().isEmpty()) {
            return findByDestinationCountryId(destinationCountryId);
        }
        return findAll().take(50); // Return first 50 if no criteria provided
    }

    // Alternative individual search methods for better performance
    Flux<TrackingNumber> findByCustomerNameContainingIgnoreCase(String customerName);

    Flux<TrackingNumber> findByCustomerSlugContainingIgnoreCase(String customerSlug);

    Flux<TrackingNumber> findByOriginCountryId(String originCountryId);

    Flux<TrackingNumber> findByDestinationCountryId(String destinationCountryId);

    Flux<TrackingNumber> findByOriginCountryIdAndDestinationCountryId(String originCountryId, String destinationCountryId);
}

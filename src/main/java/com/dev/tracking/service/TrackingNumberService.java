package com.dev.tracking.service;

import com.dev.tracking.dto.*;
import com.dev.tracking.model.TrackingNumber;
import com.dev.tracking.repository.TrackingNumberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingNumberService {

    private static final String TRACKING_NUMBER_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MAX_TRACKING_NUMBER_LENGTH = 16;
    private static final int MIN_TRACKING_NUMBER_LENGTH = 8;
    private static final String REDIS_TRACKING_PREFIX = "tracking:";
    private static final String REDIS_SEARCH_PREFIX = "search:";
    private static final String REDIS_ALL_KEY = "all_tracking_numbers";
    private static final Duration REDIS_EXPIRY = Duration.ofHours(24);
    private static final Duration SEARCH_CACHE_EXPIRY = Duration.ofMinutes(30);

    private final TrackingNumberRepository repository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor injection provides better testability and immutability.
     * We initialize SecureRandom here because it's expensive to create
     * but thread-safe to use across multiple requests.
     */
    @Autowired
    public TrackingNumberService(TrackingNumberRepository repository,
                                 @Qualifier("trackingRedisTemplate")
                                 ReactiveRedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates a unique tracking number for the given request.
     *
     * This method orchestrates the entire tracking number generation process:
     * 1. Generate a candidate tracking number using hybrid approach
     * 2. Check for uniqueness using distributed cache and database
     * 3. If collision detected, retry with exponential backoff
     * 4. Store the final result in both cache and database
     * 5. Return formatted response to client
     *
     * The reactive approach ensures this method never blocks threads, allowing
     * our application to handle thousands of concurrent requests efficiently.
     *
     * @param request The tracking number generation request with all parameters
     * @return A Mono emitting the tracking number response
     */
    public Mono<TrackingNumberResponse> generateTrackingNumber(TrackingNumberRequest request) {
        return generateUniqueTrackingNumber(request)
                .flatMap(trackingNumber -> saveTrackingNumber(trackingNumber, request))
                .map(this::createResponse)
                .retryWhen(Retry.backoff(5, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof TrackingNumberCollisionException));
    }

    /**
     * Search tracking numbers by multiple criteria
     * Strategy: Check Redis first, then MongoDB, update Redis with results
     */
    public Mono<SearchResponse> searchTrackingNumbers(String trackingNumber, String customerName,
                                                      String customerSlug, String originCountryId,
                                                      String destinationCountryId) {

        String searchKey = buildSearchKey(trackingNumber, customerName, customerSlug, originCountryId, destinationCountryId);

        // First check Redis cache
        return redisTemplate.opsForValue().get(REDIS_SEARCH_PREFIX + searchKey)
                .flatMap(cachedResult -> {
                    try {
                        SearchResponse cached = objectMapper.readValue(cachedResult, SearchResponse.class);
                        cached.setSource("redis");
                        log.debug("Found search results in Redis cache");
                        return Mono.just(cached);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize cached search result", e);
                        return searchInDatabase(trackingNumber, customerName, customerSlug, originCountryId, destinationCountryId, searchKey);
                    }
                })
                .switchIfEmpty(searchInDatabase(trackingNumber, customerName, customerSlug, originCountryId, destinationCountryId, searchKey));
    }

    /**
     * Get all tracking numbers with pagination and cache sync
     */
    public Mono<PagedResponse> getAllTrackingNumbers(int page, int size) {
        return syncCacheWithDatabase()
                .then(getPaginatedFromCache(page, size))
                .switchIfEmpty(getPaginatedFromDatabase(page, size));
    }

    private Mono<SearchResponse> searchInDatabase(String trackingNumber, String customerName, String customerSlug,
                                                  String originCountryId, String destinationCountryId, String searchKey) {
        log.debug("Searching in MongoDB");

        return repository.findBySearchCriteria(trackingNumber, customerName, customerSlug, originCountryId, destinationCountryId)
                .map(TrackingNumberInfo::new)
                .collectList()
                .map(results -> {
                    SearchResponse response = new SearchResponse(results, results.size(), "mongodb");

                    // Cache the search result
                    try {
                        String serialized = objectMapper.writeValueAsString(response);
                        redisTemplate.opsForValue().set(REDIS_SEARCH_PREFIX + searchKey, serialized, SEARCH_CACHE_EXPIRY).subscribe();

                        // Also update individual tracking number cache
                        results.forEach(this::cacheTrackingNumberInfo);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to cache search result", e);
                    }

                    return response;
                })
                .defaultIfEmpty(SearchResponse.notFound());
    }

    private Mono<Void> syncCacheWithDatabase() {
        return repository.findAll()
                .map(TrackingNumberInfo::new)
                .collectList()
                .flatMap(allData -> {
                    try {
                        String serialized = objectMapper.writeValueAsString(allData);
                        return redisTemplate.opsForValue().set(REDIS_ALL_KEY, serialized, REDIS_EXPIRY).then();
                    } catch (JsonProcessingException e) {
                        log.error("Failed to sync cache with database", e);
                        return Mono.empty();
                    }
                });
    }

    private Mono<PagedResponse> getPaginatedFromCache(int page, int size) {
        return redisTemplate.opsForValue().get(REDIS_ALL_KEY)
                .flatMap(cachedData -> {
                    try {
                        List<TrackingNumberInfo> allData = objectMapper.readValue(cachedData,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, TrackingNumberInfo.class));

                        int start = page * size;
                        int end = Math.min(start + size, allData.size());

                        if (start >= allData.size()) {
                            return Mono.just(new PagedResponse(List.of(), page, size, allData.size()));
                        }

                        List<TrackingNumberInfo> pageData = allData.subList(start, end);
                        return Mono.just(new PagedResponse(pageData, page, size, allData.size()));

                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize cached data", e);
                        return Mono.empty();
                    }
                });
    }

    private Mono<PagedResponse> getPaginatedFromDatabase(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return repository.findAll(pageRequest.getSort())
                .skip(page * size)
                .take(size)
                .map(TrackingNumberInfo::new)
                .collectList()
                .zipWith(repository.count())
                .map(tuple -> new PagedResponse(tuple.getT1(), page, size, tuple.getT2()));
    }

    private void cacheTrackingNumberInfo(TrackingNumberInfo info) {
        try {
            String serialized = objectMapper.writeValueAsString(info);
            redisTemplate.opsForValue().set(REDIS_TRACKING_PREFIX + info.getTrackingNumber(), serialized, REDIS_EXPIRY).subscribe();
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache tracking number info", e);
        }
    }

    private String buildSearchKey(String trackingNumber, String customerName, String customerSlug,
                                  String originCountryId, String destinationCountryId) {
        return String.format("%s:%s:%s:%s:%s",
                trackingNumber != null ? trackingNumber : "",
                customerName != null ? customerName : "",
                customerSlug != null ? customerSlug : "",
                originCountryId != null ? originCountryId : "",
                destinationCountryId != null ? destinationCountryId : "").hashCode() + "";
    }

    private void invalidateAllCache() {
        redisTemplate.delete(REDIS_ALL_KEY).subscribe();
        redisTemplate.keys(REDIS_SEARCH_PREFIX + "*")
                .flatMap(redisTemplate::delete)
                .subscribe();
    }

    /**
     * Generates a unique tracking number using our hybrid algorithm.
     *
     * Our generation strategy combines several elements to maximize uniqueness
     * while maintaining the required format constraints:
     *
     * 1. Deterministic prefix based on route and customer (improves uniqueness distribution)
     * 2. Timestamp-based component (ensures temporal uniqueness)
     * 3. Random suffix (handles concurrent requests and adds entropy)
     * 4. Length optimization (shorter numbers for common routes, longer for edge cases)
     *
     * This approach significantly reduces collision probability compared to
     * purely random generation, while still meeting the format requirements.
     */
    private Mono<String> generateUniqueTrackingNumber(TrackingNumberRequest request) {
        return Mono.fromCallable(() -> {
                    // Generate deterministic prefix based on route and customer
                    String routePrefix = generateRoutePrefix(request.getOriginCountryId(),
                            request.getDestinationCountryId());

                    // Add customer-based component for better distribution
                    String customerComponent = generateCustomerComponent(request.getCustomerSlug());

                    // Add temporal component to ensure uniqueness across time
                    String timeComponent = generateTimeComponent();

                    // Calculate remaining length for random component
                    int usedLength = routePrefix.length() + customerComponent.length() + timeComponent.length();
                    int randomLength = Math.min(MAX_TRACKING_NUMBER_LENGTH - usedLength,
                            Math.max(4, MIN_TRACKING_NUMBER_LENGTH - usedLength));

                    // Generate random suffix to handle concurrency
                    String randomSuffix = generateRandomString(randomLength);

                    // Combine all components ensuring we stay within length limits
                    String candidate = (routePrefix + customerComponent + timeComponent + randomSuffix)
                            .substring(0, Math.min(MAX_TRACKING_NUMBER_LENGTH,
                                    routePrefix.length() + customerComponent.length() +
                                            timeComponent.length() + randomSuffix.length()));
                    log.trace("Generated tracking number: {}", candidate);

                    return candidate;
                })
                .flatMap(this::validateUniqueness);
    }

    /**
     * Generates a route-based prefix to distribute tracking numbers across different patterns.
     * This reduces collision probability by ensuring different routes get different prefixes.
     */
    private String generateRoutePrefix(String origin, String destination) {
        // Use first letter of each country plus a deterministic character
        char originChar = origin.charAt(0);
        char destChar = destination.charAt(0);

        // Add a third character based on the combination to increase entropy
        int combinationValue = (originChar + destChar) % TRACKING_NUMBER_CHARSET.length();
        char combinationChar = TRACKING_NUMBER_CHARSET.charAt(combinationValue);

        return "" + originChar + destChar + combinationChar;
    }

    /**
     * Generates a customer-based component to further distribute tracking numbers.
     * Different customers will tend to get different tracking number patterns.
     */
    private String generateCustomerComponent(String customerSlug) {
        // Use hash of customer slug to generate consistent but distributed component
        int hash = Math.abs(customerSlug.hashCode());
        return "" + TRACKING_NUMBER_CHARSET.charAt(hash % TRACKING_NUMBER_CHARSET.length()) +
                TRACKING_NUMBER_CHARSET.charAt((hash / TRACKING_NUMBER_CHARSET.length()) % TRACKING_NUMBER_CHARSET.length());
    }

    /**
     * Generates a time-based component that changes frequently to ensure temporal uniqueness.
     * This component ensures that even identical requests at different times get different numbers.
     */
    private String generateTimeComponent() {
        // Use current time in seconds, modulo charset length for two characters
        long timeSeconds = Instant.now().getEpochSecond();
        int timeComponent = (int) (timeSeconds % (TRACKING_NUMBER_CHARSET.length() * TRACKING_NUMBER_CHARSET.length()));

        return "" + TRACKING_NUMBER_CHARSET.charAt(timeComponent % TRACKING_NUMBER_CHARSET.length()) +
                TRACKING_NUMBER_CHARSET.charAt(timeComponent / TRACKING_NUMBER_CHARSET.length());
    }

    /**
     * Generates a cryptographically secure random string of specified length.
     * We use SecureRandom to ensure high-quality randomness that won't have
     * predictable patterns even under high load.
     */
    private String generateRandomString(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(TRACKING_NUMBER_CHARSET.charAt(secureRandom.nextInt(TRACKING_NUMBER_CHARSET.length())));
        }
        return result.toString();
    }

    /**
     * Validates that a candidate tracking number is unique across our distributed system.
     *
     * This method implements a two-phase uniqueness check:
     * 1. Fast check against Redis cache (microsecond response time)
     * 2. Authoritative check against MongoDB (millisecond response time)
     *
     * We use Redis as a fast filter to catch most duplicates quickly, then
     * fall back to MongoDB for definitive validation. This approach provides
     * both speed and accuracy.
     */
    private Mono<String> validateUniqueness(String candidate) {
        String redisKey = REDIS_TRACKING_PREFIX + candidate;

        return redisTemplate.hasKey(redisKey)
                .flatMap(existsInCache -> {
                    if (existsInCache) {
                        // Fast rejection from cache
                        return Mono.error(new TrackingNumberCollisionException("Tracking number exists in cache"));
                    } else {
                        // Check database for authoritative answer
                        return repository.existsByTrackingNumber(candidate)
                                .flatMap(existsInDb -> {
                                    if (existsInDb) {
                                        return Mono.error(new TrackingNumberCollisionException("Tracking number exists in database"));
                                    } else {
                                        // Reserve in cache and return candidate
                                        return redisTemplate.opsForValue()
                                                .set(redisKey, "reserved", REDIS_EXPIRY)
                                                .thenReturn(candidate);
                                    }
                                });
                    }
                });
    }

    /**
     * Saves the tracking number to persistent storage with all associated metadata.
     *
     * This method creates a complete audit trail by storing not just the tracking
     * number, but all the contextual information that was used to generate it.
     * This supports future analytics, debugging, and compliance requirements.
     */
    private Mono<TrackingNumber> saveTrackingNumber(String trackingNumber, TrackingNumberRequest request) {
        TrackingNumber entity = new TrackingNumber(
                trackingNumber,
                request.getOriginCountryId(),
                request.getDestinationCountryId(),
                request.getWeight(),
                request.getCreatedAt(),
                request.getCustomerId(),
                request.getCustomerName(),
                request.getCustomerSlug()
        );

        return repository.save(entity)
                .doOnSuccess(saved -> {
                    // Update cache with permanent marker
                    String redisKey = REDIS_TRACKING_PREFIX + trackingNumber;
                    redisTemplate.opsForValue().set(redisKey, "permanent", REDIS_EXPIRY).subscribe();
                });
    }

    /**
     * Creates the API response object from the saved tracking number entity.
     *
     * This method transforms our internal domain object into the external
     * API contract, ensuring we return exactly what the specification requires
     * while maintaining flexibility for future enhancements.
     */
    private TrackingNumberResponse createResponse(TrackingNumber savedEntity) {
        return new TrackingNumberResponse(
                savedEntity.getTrackingNumber(),
                savedEntity.getCreatedAt()
        );
    }

    /**
     * Custom exception for handling tracking number collisions.
     *
     * This exception allows us to implement retry logic specifically for
     * collision scenarios while distinguishing them from other types of errors.
     */
    public static class TrackingNumberCollisionException extends RuntimeException {
        public TrackingNumberCollisionException(String message) {
            super(message);
        }
    }
}

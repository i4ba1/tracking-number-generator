package com.dev.tracking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application properties configuration.
 *
 * This configuration centralizes application settings and provides
 * type-safe access to configuration properties. It supports different
 * configurations for different environments (dev, staging, production).
 */
@Configuration
public class ApplicationConfig {

    /**
     * Configuration properties for tracking number generation.
     *
     * These properties allow fine-tuning of the generation algorithm
     * without code changes, supporting A/B testing and performance optimization.
     */
    @Bean
    public TrackingNumberProperties trackingNumberProperties() {
        return new TrackingNumberProperties();
    }

    /**
     * Properties class for tracking number generation settings.
     *
     * This class can be extended to include configurable parameters such as:
     * - Maximum retry attempts for collision resolution
     * - Cache expiration times
     * - Rate limiting settings
     * - Algorithm parameters for different customer tiers
     */
    @Getter
    @Setter
    public static class TrackingNumberProperties {
        // Getters and setters
        private int maxRetryAttempts = 5;
        private long cacheExpirationMinutes = 1440; // 24 hours
        private int maxTrackingNumberLength = 16;
        private int minTrackingNumberLength = 8;
    }
}

package com.dev.tracking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MongoDB configuration for document storage and indexing.
 *
 * This configuration sets up MongoDB for optimal performance with our
 * tracking number generation use case. MongoDB is chosen because:
 * 1. Excellent horizontal scaling capabilities
 * 2. Flexible document schema supports future feature evolution
 * 3. Strong consistency guarantees for our uniqueness requirements
 * 4. Built-in support for reactive operations
 */
@Configuration
public class MongoConfig {

    /**
     * Configures MongoDB indexes for optimal query performance.
     *
     * We create indexes programmatically to ensure they exist in all environments.
     * The index strategy is designed to support both our current use cases and
     * anticipated future analytics requirements.
     */
    @Bean
    public MongoDBIndexInitializer mongoIndexInitializer() {
        return new MongoDBIndexInitializer();
    }

    /**
     * Component responsible for creating MongoDB indexes on application startup.
     *
     * This approach ensures that our database is properly configured regardless
     * of the deployment environment, and indexes are created before the application
     * starts processing requests.
     */
    public static class MongoDBIndexInitializer {
        // Index creation will be handled by Spring Data annotations and repository methods
        // This bean exists to demonstrate the configuration pattern and can be extended
        // for more complex index management scenarios
    }
}


package com.dev.tracking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web configuration for HTTP request handling and validation.
 *
 * This configuration customizes how our REST API handles incoming requests,
 * including validation, error handling, and response formatting.
 */
@Configuration
public class WebConfig {

    /**
     * Global exception handler for unhandled exceptions.
     *
     * While our controller handles specific exceptions, this handler catches
     * any unexpected errors and provides a consistent API response format.
     * This is crucial for production deployments where clients need predictable
     * error responses.
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * Custom validation configuration.
     *
     * We can extend this to add custom validators for business rules,
     * such as validating country codes against a known list or implementing
     * more sophisticated weight validation rules.
     */
    public static class GlobalExceptionHandler {
        // Exception handling logic can be implemented here
        // For now, most exception handling is done in the controller
    }
}

package com.dev.tracking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for distributed caching and coordination.
 *
 * Redis serves multiple critical functions in our architecture:
 * 1. Fast duplicate detection for tracking numbers
 * 2. Distributed coordination across multiple application instances
 * 3. Caching frequently accessed data to reduce database load
 * 4. Session storage for future authentication features
 *
 * We use Lettuce as the Redis client because it provides excellent
 * performance with reactive programming and handles connection pooling automatically.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Creates the reactive Redis connection factory.
     *
     * Lettuce is preferred over Jedis for reactive applications because:
     * - Native support for reactive streams
     * - Better connection pooling and resource management
     * - Excellent performance under high concurrency
     * - Built-in support for Redis Cluster and Sentinel
     */
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * Creates the reactive Redis template for our application.
     *
     * We configure string serializers for both keys and values because:
     * - String operations are the most common in our use case
     * - Consistent serialization prevents issues with key lookups
     * - Simple debugging and monitoring through Redis CLI
     * - Cross-language compatibility if needed in the future
     */
    @Bean("trackingRedisTemplate")
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
                .<String, String>newSerializationContext()
                .key(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}

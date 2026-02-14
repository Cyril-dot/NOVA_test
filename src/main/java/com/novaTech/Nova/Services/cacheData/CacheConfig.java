package com.novaTech.Nova.Services.cacheData;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /**
     * Primary cache manager using Caffeine for better performance and TTL support
     */
    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                // User & Authentication
                "users",
                "auth-tokens",

                // Teams
                "teams",

                // Projects
                "teamProjects",
                "projects",
                "projectDocuments",

                // Tasks
                "teamTasks",
                "userTasks",

                // Reminders
                "teamReminders",

                // Documents
                "documents",
                "documentViews",

                // Chat & AI
                "chats",

                // Admin
                "adminCache",

                // Search & Temporary Data
                "search-results",
                "shortLivedCache"
        );

        // Configure Caffeine with TTL and size limits
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)                    // Max 10,000 entries per cache
                .expireAfterWrite(30, TimeUnit.MINUTES) // Default TTL: 30 minutes
                .recordStats());                        // Enable cache statistics

        // Allow dynamic cache creation for unknown cache names
        cacheManager.setAllowNullValues(false);

        return cacheManager;
    }

    /**
     * Short-lived cache manager for time-sensitive data (overdue tasks, etc.)
     * Uses shorter TTL to ensure data freshness
     */
    @Bean("shortLivedCacheManager")
    public CacheManager shortLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("shortLivedCache");

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(5, TimeUnit.MINUTES) // 5-minute TTL for time-sensitive data
                .recordStats());

        cacheManager.setAllowNullValues(false);

        return cacheManager;
    }

    /**
     * Custom cache manager for specific use cases (backwards compatibility)
     */
    @Bean("customCacheManager")
    public CacheManager customCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("users"),
                new ConcurrentMapCache("products"),
                new ConcurrentMapCache("search-results"),
                new ConcurrentMapCache("auth-tokens"),
                new ConcurrentMapCache("rate-limits")
        ));

        return cacheManager;
    }

    /**
     * Default cache resolver - uses primary cache manager
     */
    @Bean
    @Override
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(cacheManager());
    }

    /**
     * Custom key generator that includes class name, method name, and parameters
     * Format: ClassName_methodName_param1_param2_...
     */
    @Bean("customKeyGenerator")
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append("_");
            sb.append(method.getName());

            for (Object param : params) {
                sb.append("_");
                if (param != null) {
                    sb.append(param.toString());
                } else {
                    sb.append("null");
                }
            }

            return sb.toString();
        };
    }

    /**
     * Simple key generator for basic caching needs
     * Returns single param directly, or composite for multiple params
     */
    @Bean("simpleKeyGenerator")
    public KeyGenerator simpleKeyGenerator() {
        return (target, method, params) -> {
            if (params.length == 0) {
                return "EMPTY_KEY";
            }
            if (params.length == 1) {
                return params[0] == null ? "NULL_KEY" : params[0];
            }
            // For multiple params, create composite key
            return Arrays.toString(params);
        };
    }

    /**
     * Hash-based key generator for complex objects
     * Uses method name + hash of parameters
     */
    @Bean("hashKeyGenerator")
    public KeyGenerator hashKeyGenerator() {
        return (target, method, params) -> {
            int hashCode = Arrays.deepHashCode(params);
            return method.getName() + "_" + hashCode;
        };
    }

    /**
     * Custom error handler to prevent cache failures from breaking application
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }

    /**
     * Custom cache error handler implementation
     * Logs errors but allows application to continue functioning
     */
    private static class CustomCacheErrorHandler implements CacheErrorHandler {

        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(CustomCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(RuntimeException exception,
                                        org.springframework.cache.Cache cache,
                                        Object key) {
            log.error("❌ Cache GET error in cache '{}' for key '{}': {}",
                    cache.getName(), key, exception.getMessage(), exception);
            // Don't throw - allow method to execute normally without cache
        }

        @Override
        public void handleCachePutError(RuntimeException exception,
                                        org.springframework.cache.Cache cache,
                                        Object key,
                                        Object value) {
            log.error("❌ Cache PUT error in cache '{}' for key '{}': {}",
                    cache.getName(), key, exception.getMessage(), exception);
            // Don't throw - method execution completes, just without caching
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception,
                                          org.springframework.cache.Cache cache,
                                          Object key) {
            log.error("❌ Cache EVICT error in cache '{}' for key '{}': {}",
                    cache.getName(), key, exception.getMessage(), exception);
            // Don't throw - eviction failure shouldn't break the application
        }

        @Override
        public void handleCacheClearError(RuntimeException exception,
                                          org.springframework.cache.Cache cache) {
            log.error("❌ Cache CLEAR error in cache '{}': {}",
                    cache.getName(), exception.getMessage(), exception);
            // Don't throw - clear failure shouldn't break the application
        }
    }
}
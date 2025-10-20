package com.twitter.mcp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String TIMELINE_PREFIX = "timeline:";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String USER_PROFILE_PREFIX = "profile:";
    
    // Timeline caching
    public void cacheTimeline(String userId, String timelineType, Object timelineData) {
        String key = TIMELINE_PREFIX + userId + ":" + timelineType;
        redisTemplate.opsForValue().set(key, timelineData, Duration.ofMinutes(15));
        log.debug("Cached timeline for user: {} type: {}", userId, timelineType);
    }
    
    public Object getCachedTimeline(String userId, String timelineType) {
        String key = TIMELINE_PREFIX + userId + ":" + timelineType;
        return redisTemplate.opsForValue().get(key);
    }
    
    public void evictTimeline(String userId, String timelineType) {
        String key = TIMELINE_PREFIX + userId + ":" + timelineType;
        redisTemplate.delete(key);
        log.debug("Evicted timeline cache for user: {} type: {}", userId, timelineType);
    }
    
    // Rate limiting
    public boolean isRateLimited(String clientId, String operation) {
        String key = RATE_LIMIT_PREFIX + clientId + ":" + operation;
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            redisTemplate.opsForValue().set(key, 1, Duration.ofHours(1));
            return false;
        }
        
        // check if limit exceeded (assuming 1000 per hour limit)
        if (count >= 1000) {
            return true;
        }
        
        redisTemplate.opsForValue().increment(key);
        return false;
    }
    
    public int getRateLimitCount(String clientId, String operation) {
        String key = RATE_LIMIT_PREFIX + clientId + ":" + operation;
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        return count != null ? count : 0;
    }
    
    public void resetRateLimit(String clientId, String operation) {
        String key = RATE_LIMIT_PREFIX + clientId + ":" + operation;
        redisTemplate.delete(key);
        log.debug("Reset rate limit for client: {} operation: {}", clientId, operation);
    }
    
    // User profile caching
    public void cacheUserProfile(String userId, Object profileData) {
        String key = USER_PROFILE_PREFIX + userId;
        redisTemplate.opsForValue().set(key, profileData, Duration.ofHours(1));
        log.debug("Cached user profile for user: {}", userId);
    }
    
    public Object getCachedUserProfile(String userId) {
        String key = USER_PROFILE_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }
    
    public void evictUserProfile(String userId) {
        String key = USER_PROFILE_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Evicted user profile cache for user: {}", userId);
    }
    
    // Generic cache operations
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }
    
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    public void delete(String key) {
        redisTemplate.delete(key);
    }
    
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    public Set<String> getKeys(String pattern) {
        return redisTemplate.keys(pattern);
    }
    
    public void flushAll() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.warn("Flushed all Redis cache");
    }
}

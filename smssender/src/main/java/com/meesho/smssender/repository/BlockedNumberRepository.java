package com.meesho.smssender.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BlockedNumberRepository {
    
    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String REDIS_KEY = "blocked_numbers";

    public Boolean isBlocked(String mobileNumber) {
        return redisTemplate.opsForSet().isMember(REDIS_KEY, mobileNumber);
    }

    public Long blockNumber(String mobileNumber) {
        return redisTemplate.opsForSet().add(REDIS_KEY, mobileNumber);
    }

    public Long unblockNumber(String mobileNumber) {
        return redisTemplate.opsForSet().remove(REDIS_KEY, mobileNumber);
    }
}

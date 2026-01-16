package com.meesho.smssender.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class BlockedNumberRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private BlockedNumberRepository repository;

    @Test
    void isBlocked_shouldReturnTrue_whenInRedis() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(anyString(), eq("1234567890"))).thenReturn(true);

        Boolean result = repository.isBlocked("1234567890");
        assertThat(result).isTrue();
    }

    @Test
    void blockNumber_shouldReturn1_whenAdded() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(anyString(), eq("1234567890"))).thenReturn(1L);

        Long result = repository.blockNumber("1234567890");
        assertThat(result).isEqualTo(1L);
    }

    @Test
    void unblockNumber_shouldReturn1_whenRemoved() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove(anyString(), eq("1234567890"))).thenReturn(1L);

        Long result = repository.unblockNumber("1234567890");
        assertThat(result).isEqualTo(1L);
    }
}
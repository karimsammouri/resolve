package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.dto.response.AgentTicketResponse;
import org.hibernate.type.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Service
public class AgentTicketCacheService {
    private static final Duration TTL  = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "resolve:agent:ticket:";

    private static final Logger log = LoggerFactory.getLogger(AgentTicketCacheService.class);

    private final RedisTemplate<String, AgentTicketResponse> redisTemplate;

    public AgentTicketCacheService(RedisTemplate<String, AgentTicketResponse> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public AgentTicketResponse get(Long ticketId) {
        try {
            return redisTemplate.opsForValue().get(key(ticketId));
        } catch (RedisConnectionFailureException |
                 RedisSystemException |
                 SerializationException e) {
            log.warn("Redis cache read failed for ticket #{}", ticketId, e);

            return null;
        }
    }

    public void put(Long ticketId, AgentTicketResponse response) {
        try {
            redisTemplate.opsForValue().set(key(ticketId), response, TTL);
        } catch (RedisConnectionFailureException |
                 RedisSystemException |
                 SerializationException e) {
            log.warn("Redis cache write failed for ticket #{}", ticketId, e);
        }
    }

    public void evict(Long ticketId) {
        try {
            redisTemplate.delete(key(ticketId));
        } catch (RedisConnectionFailureException |
                 RedisSystemException |
                 SerializationException e) {
            log.error("Redis cache eviction failed for ticket #{}", ticketId, e);
        }
    }

    public void evictAfterCommit(Long ticketId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Cache eviction must me scheduled within an active transaction");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evict(ticketId);
            }
        });
    }

    private String key(Long ticketId) {
        return KEY_PREFIX + ticketId;
    }
}

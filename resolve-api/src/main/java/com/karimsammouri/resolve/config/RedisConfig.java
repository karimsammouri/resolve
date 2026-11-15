package com.karimsammouri.resolve.config;

import com.karimsammouri.resolve.dto.response.AgentTicketResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, AgentTicketResponse> ticketRedisTemplate(RedisConnectionFactory connectionFactory,
                                                                          JsonMapper jsonMapper) {
        RedisTemplate<String, AgentTicketResponse> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        // Store keys as readable strings
        template.setKeySerializer(new StringRedisSerializer());

        // Store values as JSON
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, AgentTicketResponse.class));

        template.afterPropertiesSet();

        return template;
    }
}

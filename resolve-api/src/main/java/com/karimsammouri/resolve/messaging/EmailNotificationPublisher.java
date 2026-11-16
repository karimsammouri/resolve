package com.karimsammouri.resolve.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EmailNotificationPublisher {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationPublisher.class);
    private static final String EMAIL_EXCHANGE = "resolve.email.exchange";
    private static final String EMAIL_ROUTING_KEY = "email.notification";

    private final RabbitTemplate rabbitTemplate;

    public EmailNotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(EmailNotification notification) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());

        try {
            rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, EMAIL_ROUTING_KEY, notification, correlationData);

            correlationData.getFuture().whenComplete((confirm, ex) -> {
                if (ex != null | confirm == null || !confirm.ack()) {
                    log.error("RabbitMQ failed to confirm notification for ticket #{}", notification.ticketId(), ex);
                } else if (correlationData.getReturned() != null) {
                    log.error("RabbitMQ could not route notification for ticket #{}", notification.ticketId());
                }
            });

        } catch (AmqpException ex) {
            log.error("Failed to publish email notification for ticket #{}", notification.ticketId(), ex);
        }
    }
}

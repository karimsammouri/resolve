package com.karimsammouri.emailservice.messaging;

import com.karimsammouri.emailservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationConsumer.class);

    private final EmailService emailService;

    public EmailNotificationConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "resolve.email.queue")
    public void consume(EmailNotification notification) {
        log.info("Received RabbitMQ message: {}", notification);

        switch(notification.type()) {
            case TICKET_CONFIRMATION -> emailService.sendTicketConfirmation(
                    notification.customerEmail(),
                    notification.customerName(),
                    notification.ticketId(),
                    notification.ticketSubject(),
                    notification.accessLink()
            );

            case AGENT_REPLY -> emailService.sendAgentReplyNotification(
                    notification.customerEmail(),
                    notification.customerName(),
                    notification.ticketId(),
                    notification.ticketSubject()
            );
        }
    }
}

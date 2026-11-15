package com.karimsammouri.resolve.client;

import com.karimsammouri.resolve.dto.request.SendAgentReplyNotificationRequest;
import com.karimsammouri.resolve.dto.request.SendTicketConfirmationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class EmailClient {
    private static final Logger log = LoggerFactory.getLogger(EmailClient.class);

    private final RestClient restClient;

    public EmailClient(RestClient.Builder restClientBuilder,
                       @Value("${resolve.email-service.url}") String emailServiceUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = restClientBuilder.baseUrl(emailServiceUrl).requestFactory(requestFactory).build();
    }

    public void sendTicketConfirmation(SendTicketConfirmationRequest request) {
        try {
            restClient.post().uri("/api/emails/ticket-confirmation").body(request).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Unable to confirm ticket confirmation email request for ticket #{}", request.ticketId(), ex);
        }
    }

    public void sendAgentReplyNotification(SendAgentReplyNotificationRequest request) {
        try {
            restClient.post().uri("/api/emails/agent-reply-notification").body(request).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Unable to confirm agent reply notification request for ticket #{}", request.ticketId(), ex);
        }
    }
}

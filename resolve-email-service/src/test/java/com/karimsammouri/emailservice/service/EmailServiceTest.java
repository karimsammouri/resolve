package com.karimsammouri.emailservice.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EmailServiceTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailService emailService = new EmailService(mailSender);

    @Test
    void sendTicketConfirmation_shouldSendCorrectEmail() {
        emailService.sendTicketConfirmation(
                "customer@example.com",
                "John",
                123L,
                "Cannot log in",
                "https://resolve.com/tickets/123?token=abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();

        assertArrayEquals(new String[]{"customer@example.com"}, sentMessage.getTo());
        assertEquals("We received your support request: Cannot log in (#123)", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("Hi John"));
        assertTrue(sentMessage.getText().contains("Ticket number: #123"));
        assertTrue(sentMessage.getText().contains("Subject: Cannot log in"));
        assertTrue(sentMessage.getText().contains("https://resolve.com/tickets/123?token=abc"));
    }
}

package com.karimsammouri.emailservice.controller;

import com.karimsammouri.emailservice.dto.SendAgentReplyNotificationRequest;
import com.karimsammouri.emailservice.dto.SendTicketConfirmationRequest;
import com.karimsammouri.emailservice.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/ticket-confirmation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendTicketConfirmation(@Valid @RequestBody SendTicketConfirmationRequest request) {
        emailService.sendTicketConfirmation(
                request.customerEmail(),
                request.customerName(),
                request.ticketId(),
                request.ticketSubject(),
                request.accessLink());
    }

    @PostMapping("/agent-reply-notification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendAgentReplyNotification(@Valid @RequestBody SendAgentReplyNotificationRequest request) {
        emailService.sendAgentReplyNotification(
                request.customerEmail(),
                request.customerName(),
                request.ticketId(),
                request.ticketSubject()
        );
    }
}

package com.karimsammouri.resolve.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTicketConfirmation(String customerEmail,
                                       String customerName,
                                       Long ticketId,
                                       String ticketSubject,
                                       String accessLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(customerEmail);
        message.setSubject("We received your support request: " + ticketSubject + " (#" + ticketId + ")");
        message.setText("""
                Hi %s,
                
                We've received your support request.
                
                Ticket number: #%d
                Subject: %s
                
                You can view your ticket and reply using this private link:
                %s
                
                Please keep this link private, as it provides access to your ticket.
                
                Thank you,
                Resolve Support
                """.formatted(
                customerName,
                ticketId,
                ticketSubject,
                accessLink));

        mailSender.send(message);
    }

    public void sendAgentReplyEmail(String customerEmail,
                                    String customerName,
                                    Long ticketId,
                                    String ticketSubject) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(customerEmail);
        message.setSubject("There's an update on your support request: " + ticketSubject + " (#" + ticketId + ")");
        message.setText("""
                Hi %s,
                
                An agent has replied to your support request.
                
                Ticket number: #%d
                Subject: %s
                
                To view the reply and respond, please use the private link in your original confirmation email.
                
                If you cannot find that email, please contact our support team for assistance.
                
                Thank you,
                Resolve Support
                """.formatted(
                customerName,
                ticketId,
                ticketSubject));

        mailSender.send(message);
    }
}

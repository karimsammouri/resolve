package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.dto.ViewMessageResponse;
import com.karimsammouri.resolve.dto.ViewTicketResponse;
import com.karimsammouri.resolve.entity.CustomerTicketAccess;
import com.karimsammouri.resolve.entity.Message;
import com.karimsammouri.resolve.entity.Organization;
import com.karimsammouri.resolve.entity.Ticket;
import com.karimsammouri.resolve.enums.MessageSenderType;
import com.karimsammouri.resolve.enums.TicketStatus;
import com.karimsammouri.resolve.repository.CustomerTicketAccessRepository;
import com.karimsammouri.resolve.repository.MessageRepository;
import com.karimsammouri.resolve.repository.OrganizationRepository;
import com.karimsammouri.resolve.repository.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PublicTicketService {
    private final String baseUrl;

    private final OrganizationRepository organizationRepo;
    private final TicketRepository ticketRepo;
    private final MessageRepository messageRepo;
    private final CustomerTicketAccessRepository ticketAccessRepo;

    private final TokenService tokenService;
    private final EmailService emailService;

    public PublicTicketService(@Value("${resolve.app.base-url}") String baseUrl,
                               OrganizationRepository organizationRepo,
                               TicketRepository ticketRepo,
                               MessageRepository messageRepo,
                               CustomerTicketAccessRepository ticketAccessRepo,
                               TokenService tokenService,
                               EmailService emailService) {
        this.baseUrl = baseUrl;
        this.organizationRepo = organizationRepo;
        this.ticketRepo = ticketRepo;
        this.messageRepo = messageRepo;
        this.ticketAccessRepo = ticketAccessRepo;
        this.tokenService = tokenService;
        this.emailService = emailService;
    }

    @Transactional
    public Long createTicket(String organizationSlug,
                             String customerName,
                             String customerEmail,
                             String messageSubject,
                             String messageBody) {
        // Retrieve and validate organization by slug
        Organization organization = organizationRepo.findBySlug(organizationSlug)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationSlug));

        // Create and save message and ticket with given data
        Ticket ticket = new Ticket(messageSubject, customerName, customerEmail, organization);
        ticketRepo.save(ticket);
        Message message = new Message(ticket, MessageSenderType.CUSTOMER, null, customerName, messageBody, false);
        messageRepo.save(message);

        // Create and save CustomerTicketAccess for customer
        String token = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(token);
        CustomerTicketAccess ticketAccess = new CustomerTicketAccess(ticket, tokenHash);
        ticketAccessRepo.save(ticketAccess);

        // Send confirmation email with access token if transaction commits
        Long ticketId = ticket.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String accessLink = baseUrl + "/api/public/tickets/" + ticketId + "?token=" + token;
                emailService.sendTicketConfirmation(customerEmail, customerName, ticketId, messageSubject, accessLink);
            }
        });

        return ticketId;
    }

    public ViewTicketResponse viewTicket(Long ticketId, String token) {
        // Validate customer token
        String tokenHash = tokenService.hashToken(token);
        ticketAccessRepo.findByTicket_IdAndTokenHashAndRevokedAtIsNull(ticketId, tokenHash)
                .orElseThrow(() -> new EntityNotFoundException("Invalid or expired access token"));

        // Retrieve and map messages to DTOs
        List<Message> messages = messageRepo.findByTicket_IdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticketId);
        List<ViewMessageResponse> messageResponses = messages.stream()
                .map(message -> new ViewMessageResponse(
                        message.getId(),
                        message.getSenderType().name(),
                        message.getSenderName(),
                        message.getBody(),
                        message.getCreatedAt()
                )).toList();

        // Retrieve and return ticket and associated messages
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: #" + ticketId));
        return new ViewTicketResponse(
                ticketId,
                ticket.getSubject(),
                ticket.getStatus().name(),
                ticket.getCustomerName(),
                ticket.getCustomerEmail(),
                messageResponses);
    }

    @Transactional
    public Long addMessage(Long ticketId, String token, String body) {
        // Validate customer token
        String tokenHash = tokenService.hashToken(token);
        ticketAccessRepo.findByTicket_IdAndTokenHashAndRevokedAtIsNull(ticketId, tokenHash)
                .orElseThrow(() -> new EntityNotFoundException("Invalid or expired access token"));

        // Retrieve ticket
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: #" + ticketId));

        // Prevent messages to resolved tickets
        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new IllegalStateException("Cannot add message to ticket marked resolved");
        }

        // Create and save message
        Message message = new Message(ticket, MessageSenderType.CUSTOMER, null, ticket.getCustomerName(), body, false);
        messageRepo.save(message);

        // Update ticket
        ticket.setHasUnreadCustomerMessage(true);
        ticket.setUpdatedAt(LocalDateTime.now());

        return message.getId();
    }
}

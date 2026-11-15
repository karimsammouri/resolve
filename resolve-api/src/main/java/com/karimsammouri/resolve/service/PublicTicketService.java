package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.client.EmailClient;
import com.karimsammouri.resolve.dto.request.AddMessageRequest;
import com.karimsammouri.resolve.dto.request.CreateTicketRequest;
import com.karimsammouri.resolve.dto.request.SendTicketConfirmationRequest;
import com.karimsammouri.resolve.dto.response.ViewMessageResponse;
import com.karimsammouri.resolve.dto.response.ViewTicketResponse;
import com.karimsammouri.resolve.entity.*;
import com.karimsammouri.resolve.enums.MessageSenderType;
import com.karimsammouri.resolve.enums.TicketStatus;
import com.karimsammouri.resolve.exception.InvalidTicketAccessException;
import com.karimsammouri.resolve.exception.InvalidTicketStateException;
import com.karimsammouri.resolve.exception.ResourceNotFoundException;
import com.karimsammouri.resolve.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PublicTicketService {
    private final String baseUrl;

    private final EmailClient emailClient;

    private final TokenService tokenService;
    private final AgentTicketCacheService ticketCacheService;

    private final OrganizationRepository organizationRepo;
    private final TicketRepository ticketRepo;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepo;
    private final MessageRepository messageRepo;
    private final CustomerTicketAccessRepository ticketAccessRepo;

    public PublicTicketService(@Value("${resolve.app.base-url}") String baseUrl,
                               EmailClient emailClient,
                               TokenService tokenService,
                               AgentTicketCacheService ticketCacheService,
                               OrganizationRepository organizationRepo,
                               TicketRepository ticketRepo,
                               TicketStatusHistoryRepository ticketStatusHistoryRepo,
                               MessageRepository messageRepo,
                               CustomerTicketAccessRepository ticketAccessRepo) {
        this.baseUrl = baseUrl;
        this.emailClient = emailClient;
        this.tokenService = tokenService;
        this.ticketCacheService = ticketCacheService;
        this.organizationRepo = organizationRepo;
        this.ticketRepo = ticketRepo;
        this.ticketStatusHistoryRepo = ticketStatusHistoryRepo;
        this.messageRepo = messageRepo;
        this.ticketAccessRepo = ticketAccessRepo;
    }

    @Transactional
    public Long createTicket(String organizationSlug, CreateTicketRequest request) {
        // Retrieve and validate organization by slug
        Organization organization = organizationRepo.findBySlug(organizationSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationSlug));

        // Create and save message and ticket with given data
        Ticket ticket = new Ticket(
                request.messageSubject(),
                request.customerName(),
                request.customerEmail(),
                organization);
        ticketRepo.save(ticket);
        Message message = new Message(
                ticket,
                MessageSenderType.CUSTOMER,
                null,
                request.customerName(),
                request.messageBody(),
                false);
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
                SendTicketConfirmationRequest emailRequest = new SendTicketConfirmationRequest(
                        request.customerEmail(),
                        request.customerName(),
                        ticketId,
                        request.messageSubject(),
                        accessLink
                );
                emailClient.sendTicketConfirmation(emailRequest);
            }
        });

        return ticketId;
    }

    @Transactional(readOnly = true)
    public ViewTicketResponse viewTicket(Long ticketId, String token) {
        // Validate customer token
        String tokenHash = tokenService.hashToken(token);
        ticketAccessRepo.findByTicket_IdAndTokenHashAndRevokedAtIsNull(ticketId, tokenHash)
                .orElseThrow(() -> new InvalidTicketAccessException("Invalid or expired access token"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));
        return new ViewTicketResponse(
                ticketId,
                ticket.getSubject(),
                ticket.getStatus().name(),
                ticket.getCustomerName(),
                ticket.getCustomerEmail(),
                messageResponses);
    }

    @Transactional
    public Long addMessage(Long ticketId, String token, AddMessageRequest request) {
        // Validate customer token
        String tokenHash = tokenService.hashToken(token);
        ticketAccessRepo.findByTicket_IdAndTokenHashAndRevokedAtIsNull(ticketId, tokenHash)
                .orElseThrow(() -> new InvalidTicketAccessException("Invalid or expired access token"));

        // Retrieve ticket
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));

        // Prevent messages to archived tickets; reopen ticket if marked resolved
        if (ticket.getStatus() == TicketStatus.ARCHIVED) {
            throw new InvalidTicketStateException("Cannot add message to an archived ticket");
        } else if (ticket.getStatus() == TicketStatus.RESOLVED) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setResolvedAt(null);
            ticketStatusHistoryRepo.save(
                    new TicketStatusHistory(ticket, oldStatus, TicketStatus.OPEN, null));
        }

        // Create and save message
        Message message = new Message(
                ticket,
                MessageSenderType.CUSTOMER,
                null,
                ticket.getCustomerName(),
                request.body(),
                false);
        messageRepo.save(message);

        // Update ticket
        ticket.setHasUnreadCustomerMessage(true);
        ticket.setUpdatedAt(LocalDateTime.now());

        ticketCacheService.evictAfterCommit(ticketId);

        return message.getId();
    }
}

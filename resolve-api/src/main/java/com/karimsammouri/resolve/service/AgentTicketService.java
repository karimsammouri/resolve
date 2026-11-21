package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.dto.request.AgentMessageRequest;
import com.karimsammouri.resolve.dto.response.AgentMessageResponse;
import com.karimsammouri.resolve.dto.response.AgentTicketResponse;
import com.karimsammouri.resolve.dto.response.AgentTicketSummaryResponse;
import com.karimsammouri.resolve.entity.Message;
import com.karimsammouri.resolve.entity.Ticket;
import com.karimsammouri.resolve.entity.TicketStatusHistory;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.enums.MessageSenderType;
import com.karimsammouri.resolve.enums.TicketStatus;
import com.karimsammouri.resolve.exception.InvalidTicketStateException;
import com.karimsammouri.resolve.exception.ResourceNotFoundException;
import com.karimsammouri.resolve.messaging.EmailNotification;
import com.karimsammouri.resolve.messaging.EmailNotificationPublisher;
import com.karimsammouri.resolve.messaging.EmailNotificationType;
import com.karimsammouri.resolve.repository.MessageRepository;
import com.karimsammouri.resolve.repository.TicketRepository;
import com.karimsammouri.resolve.repository.TicketStatusHistoryRepository;
import com.karimsammouri.resolve.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AgentTicketService {
    private final EmailNotificationPublisher emailPublisher;

    private final AgentTicketCacheService ticketCacheService;

    private final UserRepository userRepo;
    private final TicketRepository ticketRepo;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepo;
    private final MessageRepository messageRepo;

    public AgentTicketService(EmailNotificationPublisher emailPublisher,
                              AgentTicketCacheService ticketCacheService,
                              UserRepository userRepo,
                              TicketRepository ticketRepo,
                              MessageRepository messageRepo,
                              TicketStatusHistoryRepository ticketStatusHistoryRepo) {
        this.emailPublisher = emailPublisher;
        this.ticketCacheService = ticketCacheService;
        this.userRepo = userRepo;
        this.ticketRepo = ticketRepo;
        this.ticketStatusHistoryRepo = ticketStatusHistoryRepo;
        this.messageRepo = messageRepo;
    }

    @Transactional(readOnly = true)
    public AgentTicketResponse viewTicket(Long ticketId, Long organizationId) {
        // Check Redis for cached ticket
        AgentTicketResponse cached = ticketCacheService.get(ticketId);

        if (cached != null) {
            // Verify organization scope
            if (!organizationId.equals(cached.organizationId())) {
                throw new ResourceNotFoundException("Ticket not found: #" + ticketId);
            }

            return cached;
        }

        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));

        // Retrieve and map messages to DTOs
        List<Message> messages = messageRepo.findByTicket_IdOrderByCreatedAtAsc(ticketId);
        List<AgentMessageResponse> messageResponses = messages.stream()
                .map(message -> new AgentMessageResponse(
                        message.getId(),
                        message.getSenderType(),
                        message.getSenderName(),
                        message.getBody(),
                        message.isInternalNote(),
                        message.getCreatedAt()
                )).toList();

        // Construct response
        AgentTicketResponse response = new AgentTicketResponse(
                ticketId,
                ticket.getOrganization().getId(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getCustomerName(),
                ticket.getCustomerEmail(),
                ticket.getAssignedAgent() != null ?
                        ticket.getAssignedAgent().getName() : null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                messageResponses);

        // Store response in Redis
        ticketCacheService.put(ticketId, response);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<AgentTicketSummaryResponse> viewTickets(Long organizationId, Pageable pageable) {
        // Retrieve and return tickets scoped to user's organization
        Page<Ticket> tickets = ticketRepo.findByOrganization_Id(organizationId, pageable);

        return tickets.map(ticket -> new AgentTicketSummaryResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getCustomerName(),
                ticket.getCustomerEmail(),
                ticket.getAssignedAgent() != null ?
                        ticket.getAssignedAgent().getName() : null,
                ticket.hasUnreadCustomerMessage(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()));
    }

    @Transactional
    public AgentMessageResponse addMessage(Long ticketId,
                                           Long userId,
                                           Long organizationId,
                                           String senderName,
                                           AgentMessageRequest request) {
        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));

        // Obtain a lazy User reference without a database query
        User user = userRepo.getReferenceById(userId);

        // Prevent messages to archived tickets; reopen ticket if marked resolved
        if (ticket.getStatus() == TicketStatus.ARCHIVED) {
            throw new InvalidTicketStateException("Cannot add message to an archived ticket");
        } else if (ticket.getStatus() == TicketStatus.RESOLVED && !request.isInternalNote()) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setResolvedAt(null);
            ticketStatusHistoryRepo.save(
                    new TicketStatusHistory(ticket, oldStatus, TicketStatus.OPEN, user));
        }

        // Create and save message
        Message message = new Message(
                ticket,
                MessageSenderType.STAFF,
                user,
                senderName,
                request.body(),
                request.isInternalNote());
        messageRepo.save(message);

        // Update ticket
        ticket.setUpdatedAt(LocalDateTime.now());

        ticketCacheService.evictAfterCommit(ticketId);

        // Send email to notify customer of agent reply if not internal note
        if (!request.isInternalNote()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    EmailNotification notification = new EmailNotification(
                            EmailNotificationType.AGENT_REPLY,
                            ticket.getCustomerEmail(),
                            ticket.getCustomerName(),
                            ticketId,
                            ticket.getSubject(),
                            null);

                    emailPublisher.publish(notification);
                }
            });
        }

        return new AgentMessageResponse(
                message.getId(),
                message.getSenderType(),
                message.getSenderName(),
                message.getBody(),
                message.isInternalNote(),
                message.getCreatedAt());
    }

    @Transactional
    public void changeStatus(Long ticketId, Long userId, Long organizationId, TicketStatus targetStatus) {
        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));

        // Only update if ticket status actually changes
        if (ticket.getStatus() != targetStatus) {
            TicketStatus oldStatus = ticket.getStatus();
            LocalDateTime now = LocalDateTime.now();

            ticket.setStatus(targetStatus);
            ticket.setUpdatedAt(now);

            if (targetStatus == TicketStatus.RESOLVED) {
                ticket.setResolvedAt(now);
            } else {
                ticket.setResolvedAt(null);
            }

            // Record status transition
            User user = userRepo.getReferenceById(userId);
            ticketStatusHistoryRepo.save(
                    new TicketStatusHistory(ticket, oldStatus, targetStatus, user));

            ticketCacheService.evictAfterCommit(ticketId);
        }
    }

    @Transactional
    public void assignAgent(Long ticketId, Long organizationId, Long targetAgentId) {
        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));
        Long assignedAgentId = ticket.getAssignedAgent() != null ?
                ticket.getAssignedAgent().getId() : null;

        // Only update if assignment changes
        if (!Objects.equals(assignedAgentId, targetAgentId)) {
            User targetAgent = null;

            // Retrieve target agent if assigning (not unassigning)
            if (targetAgentId != null) {
                targetAgent = userRepo.findByIdAndOrganization_Id(targetAgentId, organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: #" + targetAgentId));
            }

            ticket.setAssignedAgent(targetAgent);
            ticket.setUpdatedAt(LocalDateTime.now());

            ticketCacheService.evictAfterCommit(ticketId);
        }
    }

    @Transactional
    public void markAsRead(Long ticketId, Long organizationId) {
        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));

        if (ticket.hasUnreadCustomerMessage()) {
            ticket.setHasUnreadCustomerMessage(false);
        }
    }
}
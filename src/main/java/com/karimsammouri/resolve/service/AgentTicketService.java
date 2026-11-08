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
    private final EmailService emailService;

    private final UserRepository userRepo;
    private final TicketRepository ticketRepo;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepo;
    private final MessageRepository messageRepo;

    public AgentTicketService(EmailService emailService,
                              UserRepository userRepo,
                              TicketRepository ticketRepo,
                              MessageRepository messageRepo,
                              TicketStatusHistoryRepository ticketStatusHistoryRepo) {
        this.emailService = emailService;
        this.userRepo = userRepo;
        this.ticketRepo = ticketRepo;
        this.ticketStatusHistoryRepo = ticketStatusHistoryRepo;
        this.messageRepo = messageRepo;
    }

    public AgentTicketResponse viewTicket(Long ticketId, User user) {
        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, user.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));

        // Mark customer messages as read
        ticket.setHasUnreadCustomerMessage(false);
        ticketRepo.save(ticket);

        // Retrieve and map messages to DTOs
        List<Message> messages = messageRepo.findByTicket_IdOrderByCreatedAtAsc(ticketId);
        List<AgentMessageResponse> messageResponses = messages.stream()
                .map(message -> new AgentMessageResponse(
                        message.getId(),
                        message.getSenderType().name(),
                        message.getSenderName(),
                        message.getBody(),
                        message.isInternalNote(),
                        message.getCreatedAt()
                )).toList();

        // Return ticket and associated messages
        return new AgentTicketResponse(
                ticketId,
                ticket.getSubject(),
                ticket.getStatus().name(),
                ticket.getCustomerName(),
                ticket.getCustomerEmail(),
                ticket.getAssignedAgent() != null ?
                        ticket.getAssignedAgent().getName() : null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                messageResponses);
    }

    public Page<AgentTicketSummaryResponse> viewTickets(User user, Pageable pageable) {
        // Retrieve and return tickets scoped to user's organization
        Page<Ticket> tickets = ticketRepo.findByOrganization_Id(user.getOrganization().getId(), pageable);

        return tickets.map(ticket -> new AgentTicketSummaryResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getStatus().name(),
                ticket.getCustomerName(),
                ticket.getCustomerEmail(),
                ticket.getAssignedAgent() != null ?
                        ticket.getAssignedAgent().getName() : null,
                ticket.hasUnreadCustomerMessage(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()));
    }

    @Transactional
    public Long addMessage(Long ticketId, User user, AgentMessageRequest request) {
        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, user.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));

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
                user.getName(),
                request.body(),
                request.isInternalNote());
        messageRepo.save(message);

        // Update ticket
        ticket.setUpdatedAt(LocalDateTime.now());

        // Send email to notify customer of agent reply if not internal note
        if (!request.isInternalNote()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.sendAgentReplyEmail(
                            ticket.getCustomerEmail(), ticket.getCustomerName(), ticketId, ticket.getSubject());
                }
            });
        }

        return message.getId();
    }

    @Transactional
    public Long changeStatus(Long ticketId, User user, TicketStatus targetStatus) {
        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, user.getOrganization().getId())
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
            ticketStatusHistoryRepo.save(
                    new TicketStatusHistory(ticket, oldStatus, targetStatus, user));
        }

        return ticketId;
    }

    @Transactional
    public Long assignAgent(Long ticketId, User user, Long targetAgentId) {
        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, user.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: #" + ticketId));
        Long assignedAgentId = ticket.getAssignedAgent() != null ?
                ticket.getAssignedAgent().getId() : null;

        // Only update if assignment changes
        if (!Objects.equals(assignedAgentId, targetAgentId)) {
            User targetAgent = null;

            // Retrieve target agent if assigning (not unassigning)
            if (targetAgentId != null) {
                targetAgent = userRepo.findByIdAndOrganization_Id(targetAgentId, user.getOrganization().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: #" + targetAgentId));
            }

            ticket.setAssignedAgent(targetAgent);
            ticket.setUpdatedAt(LocalDateTime.now());
        }

        return ticketId;
    }
}
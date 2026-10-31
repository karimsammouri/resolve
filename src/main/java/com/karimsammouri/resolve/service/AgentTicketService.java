package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.dto.AgentMessageResponse;
import com.karimsammouri.resolve.dto.AgentTicketResponse;
import com.karimsammouri.resolve.entity.Message;
import com.karimsammouri.resolve.entity.Ticket;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.repository.MessageRepository;
import com.karimsammouri.resolve.repository.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentTicketService {
    private final TicketRepository ticketRepo;
    private final MessageRepository messageRepo;

    public AgentTicketService(TicketRepository ticketRepo,
                              MessageRepository messageRepo) {
        this.ticketRepo = ticketRepo;
        this.messageRepo = messageRepo;
    }

    public AgentTicketResponse viewTicket(Long ticketId, User user) {
        // Retrieve ticket scoped to user's organization
        Ticket ticket = ticketRepo.findByIdAndOrganization_Id(ticketId, user.getOrganization().getId())
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: #" + ticketId));

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
}
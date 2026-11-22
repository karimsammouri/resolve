package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.dto.request.AgentMessageRequest;
import com.karimsammouri.resolve.dto.response.AgentMessageResponse;
import com.karimsammouri.resolve.dto.response.AgentTicketResponse;
import com.karimsammouri.resolve.dto.response.AgentTicketSummaryResponse;
import com.karimsammouri.resolve.entity.*;
import com.karimsammouri.resolve.enums.MessageSenderType;
import com.karimsammouri.resolve.enums.TicketStatus;
import com.karimsammouri.resolve.enums.UserRole;
import com.karimsammouri.resolve.exception.InvalidTicketStateException;
import com.karimsammouri.resolve.exception.ResourceNotFoundException;
import com.karimsammouri.resolve.messaging.EmailNotification;
import com.karimsammouri.resolve.messaging.EmailNotificationPublisher;
import com.karimsammouri.resolve.messaging.EmailNotificationType;
import com.karimsammouri.resolve.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AgentTicketServiceTest {
    private final EmailNotificationPublisher emailPublisher = mock(EmailNotificationPublisher.class);

    private final AgentTicketCacheService ticketCacheService = mock(AgentTicketCacheService.class);

    private final UserRepository userRepo = mock(UserRepository.class);
    private final TicketRepository ticketRepo = mock(TicketRepository.class);
    private final TicketStatusHistoryRepository ticketStatusHistoryRepo = mock(TicketStatusHistoryRepository.class);
    private final MessageRepository messageRepo = mock(MessageRepository.class);

    private final AgentTicketService agentTicketService = new AgentTicketService(
            emailPublisher,
            ticketCacheService,
            userRepo,
            ticketRepo,
            messageRepo,
            ticketStatusHistoryRepo
    );

    private Long ticketId;
    private Long organizationId;
    private Long userId;
    private Organization organization;
    private Ticket ticket;
    private User user;

    @BeforeEach
    void setup() {
        ticketId = 123L;
        organizationId = 1L;
        userId = 2L;

        organization = new Organization("Acme", "acme");
        ReflectionTestUtils.setField(organization, "id", organizationId);

        ticket = new Ticket(
                "Cannot log in",
                "John Smith",
                "johnsmith@example.com",
                organization);

        user = new User(
                "Agent Smith",
                "agent@acme.com",
                "hashed-password",
                UserRole.AGENT,
                organization);
        ReflectionTestUtils.setField(user, "id", 2L);

        when(ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)).thenReturn(Optional.of(ticket));
        when(userRepo.getReferenceById(userId)).thenReturn(user);
    }

    @Test
    void viewTicket_shouldReturnTicketAndMessages() {
        Message message = new Message(
                ticket,
                MessageSenderType.CUSTOMER,
                null,
                "John Smith",
                "I cannot log in",
                false);

        when(messageRepo.findByTicket_IdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of(message));

        AgentTicketResponse response = agentTicketService.viewTicket(ticketId, organizationId);

        assertEquals(ticketId, response.ticketId());
        assertEquals("Cannot log in", response.subject());
        assertEquals(TicketStatus.OPEN, response.status());
        assertEquals("John Smith", response.customerName());
        assertEquals("johnsmith@example.com", response.customerEmail());
        assertNull(response.assignedAgentName());
        assertEquals(1, response.messages().size());

        AgentMessageResponse returnedMessage = response.messages().get(0);

        assertEquals(MessageSenderType.CUSTOMER, returnedMessage.senderType());
        assertEquals("John Smith", returnedMessage.senderName());
        assertEquals("I cannot log in", returnedMessage.body());
        assertFalse(returnedMessage.isInternalNote());

        verify(ticketCacheService).get(ticketId);
        verify(ticketCacheService).put(ticketId, response);
        verify(ticketRepo, never()).save(any(Ticket.class));
    }

    @Test
    void viewTicket_withCacheHit_shouldReturnCachedResponse() {
        AgentTicketResponse cachedResponse = mock(AgentTicketResponse.class);

        when(ticketCacheService.get(ticketId)).thenReturn(cachedResponse);
        when(cachedResponse.organizationId()).thenReturn(organizationId);

        AgentTicketResponse result = agentTicketService.viewTicket(ticketId, organizationId);

        assertSame(cachedResponse, result);

        verifyNoInteractions(ticketRepo, messageRepo);
    }

    @Test
    void viewTicket_withCacheFromDifferentOrganization_shouldRejectAccess() {
        AgentTicketResponse cachedResponse = mock(AgentTicketResponse.class);

        when(ticketCacheService.get(ticketId)).thenReturn(cachedResponse);
        when(cachedResponse.organizationId()).thenReturn(2L);

        assertThrows(ResourceNotFoundException.class,
                () -> agentTicketService.viewTicket(ticketId, organizationId));
    }

    @Test
    void viewTicket_shouldRejectInvalidTicket() {
        when(ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> agentTicketService.viewTicket(ticketId, organizationId));

        verify(messageRepo, never()).findByTicket_IdOrderByCreatedAtAsc(ticketId);
    }

    @Test
    void viewTickets_shouldReturnTickets() {
        Ticket ticket1 = new Ticket(
                "Cannot log in",
                "John Smith",
                "johnsmith@example.com",
                organization);

        Ticket ticket2 = new Ticket(
                "Payment failed",
                "Jane Doe",
                "jane@example.com",
                organization);
        ticket2.setHasUnreadCustomerMessage(false);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket1, ticket2), pageable, 5);

        when(ticketRepo.findByOrganization_Id(organizationId, pageable))
                .thenReturn(ticketPage);

        Page<AgentTicketSummaryResponse> result =
                agentTicketService.viewTickets(organizationId, pageable);
        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());

        AgentTicketSummaryResponse first = result.getContent().get(0);
        assertEquals("Cannot log in", first.subject());
        assertEquals(TicketStatus.OPEN, first.status());
        assertEquals("John Smith", first.customerName());
        assertEquals("johnsmith@example.com", first.customerEmail());
        assertNull(first.assignedAgentName());
        assertTrue(first.hasUnreadCustomerMessage());
        assertNotNull(first.createdAt());
        assertNotNull(first.updatedAt());

        AgentTicketSummaryResponse second = result.getContent().get(1);
        assertEquals("Payment failed", second.subject());
        assertEquals("Jane Doe", second.customerName());
        assertFalse(second.hasUnreadCustomerMessage());
    }

    @Test
    void addMessage_shouldSavePublicReplyAndScheduleEmailNotification() {
        LocalDateTime previousUpdatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        ticket.setUpdatedAt(previousUpdatedAt);

        AgentMessageRequest request = new AgentMessageRequest("We have fixed the issue.", false);

        TransactionSynchronizationManager.initSynchronization();

        try {
            agentTicketService.addMessage(ticketId, userId, organizationId, "Agent Smith", request);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

            verify(messageRepo).save(messageCaptor.capture());

            Message message = messageCaptor.getValue();

            assertEquals(ticket, message.getTicket());
            assertEquals(MessageSenderType.STAFF, message.getSenderType());
            assertEquals(user, message.getSenderUser());
            assertEquals("Agent Smith", message.getSenderName());
            assertEquals("We have fixed the issue.", message.getBody());
            assertFalse(message.isInternalNote());

            assertTrue(ticket.getUpdatedAt().isAfter(previousUpdatedAt));

            // Email notification must not be published before the transaction commits
            verifyNoInteractions(emailPublisher);

            // Simulate transaction commit
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            ArgumentCaptor<EmailNotification> notificationCaptor =
                    ArgumentCaptor.forClass(EmailNotification.class);

            verify(emailPublisher).publish(notificationCaptor.capture());

            EmailNotification notification = notificationCaptor.getValue();

            assertEquals(EmailNotificationType.AGENT_REPLY, notification.type());
            assertEquals("johnsmith@example.com", notification.customerEmail());
            assertEquals("John Smith", notification.customerName());
            assertEquals(ticketId, notification.ticketId());
            assertEquals("Cannot log in", notification.ticketSubject());

            verify(ticketCacheService).evictAfterCommit(ticketId);

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void addMessage_shouldSaveInternalNote() {
        LocalDateTime previousUpdatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        ticket.setUpdatedAt(previousUpdatedAt);

        AgentMessageRequest request = new AgentMessageRequest("Internal investigation notes.", true);

        TransactionSynchronizationManager.initSynchronization();

        try {
            agentTicketService.addMessage(ticketId, userId, organizationId, "Agent Smith", request);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

            verify(messageRepo).save(messageCaptor.capture());

            Message message = messageCaptor.getValue();

            assertEquals(ticket, message.getTicket());
            assertEquals(MessageSenderType.STAFF, message.getSenderType());
            assertEquals(user, message.getSenderUser());
            assertEquals("Agent Smith", message.getSenderName());
            assertEquals("Internal investigation notes.", message.getBody());
            assertTrue(message.isInternalNote());

            assertTrue(ticket.getUpdatedAt().isAfter(previousUpdatedAt));

            // Simulate transaction commit
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            // Verify no email notification was published
            verifyNoInteractions(emailPublisher);
            verify(ticketCacheService).evictAfterCommit(ticketId);

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void addMessage_shouldReopenResolvedTicketForPublicReply() {
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.of(2025, 1, 1, 0, 0));

        AgentMessageRequest request = new AgentMessageRequest("Following up on this issue.", false);

        TransactionSynchronizationManager.initSynchronization();

        try {
            agentTicketService.addMessage(ticketId, userId, organizationId, "Agent Smith", request);

            assertEquals(TicketStatus.OPEN, ticket.getStatus());
            assertNull(ticket.getResolvedAt());

            verify(ticketStatusHistoryRepo).save(any(TicketStatusHistory.class));
            verify(messageRepo).save(any(Message.class));

            // Email notification must not be published before the transaction commits
            verifyNoInteractions(emailPublisher);

            // Simulate transaction commit
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            verify(emailPublisher).publish(any(EmailNotification.class));
            verify(ticketCacheService).evictAfterCommit(ticketId);

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void addMessage_shouldRejectRepliesOnArchivedTicket() {
        ticket.setStatus(TicketStatus.ARCHIVED);

        AgentMessageRequest request = new AgentMessageRequest("Following up on this issue.", false);

        assertThrows(InvalidTicketStateException.class, () -> agentTicketService.addMessage(
                ticketId,
                userId,
                organizationId,
                "Agent Smith",
                request));

        verify(ticketStatusHistoryRepo, never()).save(any(TicketStatusHistory.class));
        verify(messageRepo, never()).save(any(Message.class));
        verify(ticketCacheService, never()).evictAfterCommit(ticketId);
    }

    @Test
    void changeStatus_shouldChangeTicketStatus() {
        assertEquals(TicketStatus.OPEN, ticket.getStatus());

        LocalDateTime previousUpdatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        ticket.setUpdatedAt(previousUpdatedAt);

        agentTicketService.changeStatus(ticketId, userId, organizationId, TicketStatus.RESOLVED);

        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());
        assertTrue(ticket.getUpdatedAt().isAfter(previousUpdatedAt));
        assertNotNull(ticket.getResolvedAt());

        ArgumentCaptor<TicketStatusHistory> captor = ArgumentCaptor.forClass(TicketStatusHistory.class);

        verify(ticketStatusHistoryRepo).save(captor.capture());

        TicketStatusHistory statusHistory = captor.getValue();
        assertEquals(TicketStatus.OPEN, statusHistory.getOldStatus());
        assertEquals(TicketStatus.RESOLVED, statusHistory.getNewStatus());
        assertEquals(user, statusHistory.getUpdatedByUser());

        verify(ticketCacheService).evictAfterCommit(ticketId);
    }

    @Test
    void changeStatus_shouldRejectInvalidTicket() {
        assertEquals(TicketStatus.OPEN, ticket.getStatus());

        when(ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> agentTicketService.changeStatus(ticketId, userId, organizationId, TicketStatus.RESOLVED));

        assertEquals(TicketStatus.OPEN, ticket.getStatus());

        verify(ticketStatusHistoryRepo, never()).save(any(TicketStatusHistory.class));
        verify(ticketCacheService, never()).evictAfterCommit(ticketId);
    }

    @Test
    void assignAgent_shouldAssignAgentToTicket() {
        assertNull(ticket.getAssignedAgent());

        Long targetAgentId = 1L;

        LocalDateTime previousUpdatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        ticket.setUpdatedAt(previousUpdatedAt);

        when(userRepo.findByIdAndOrganization_Id(targetAgentId, organizationId)).thenReturn(Optional.of(user));

        agentTicketService.assignAgent(ticketId, organizationId, targetAgentId);

        assertEquals(user, ticket.getAssignedAgent());
        assertTrue(ticket.getUpdatedAt().isAfter(previousUpdatedAt));

        verify(ticketCacheService).evictAfterCommit(ticketId);
    }

    @Test
    void assignAgent_withNullAgentId_shouldUnassignAgent() {
        ticket.setAssignedAgent(user);

        agentTicketService.assignAgent(ticketId, organizationId, null);

        assertNull(ticket.getAssignedAgent());

        verifyNoInteractions(userRepo);
        verify(ticketCacheService).evictAfterCommit(ticketId);
    }

    @Test
    void assignAgent_shouldRejectInvalidTicket() {
        assertNull(ticket.getAssignedAgent());

        Long targetAgentId = 1L;

        when(ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> agentTicketService.assignAgent(ticketId, organizationId, targetAgentId));

        assertNull(ticket.getAssignedAgent());

        verify(ticketCacheService, never()).evictAfterCommit(ticketId);
    }

    @Test
    void assignAgent_shouldRejectInvalidAgent() {
        assertNull(ticket.getAssignedAgent());

        Long targetAgentId = 1L;

        when(userRepo.findByIdAndOrganization_Id(targetAgentId, organizationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> agentTicketService.assignAgent(ticketId, organizationId, targetAgentId));

        assertNull(ticket.getAssignedAgent());

        verify(ticketCacheService, never()).evictAfterCommit(ticketId);
    }

    @Test
    void markAsRead_shouldClearUnreadFlag() {
        ticket.setHasUnreadCustomerMessage(true);

        agentTicketService.markAsRead(ticketId, organizationId);

        assertFalse(ticket.hasUnreadCustomerMessage());
    }

    @Test
    void markAsRead_shouldRejectInvalidTicket() {
        when(ticketRepo.findByIdAndOrganization_Id(ticketId, organizationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> agentTicketService.markAsRead(ticketId, organizationId));
    }
}

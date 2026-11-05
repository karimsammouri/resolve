package com.karimsammouri.resolve.service;

import com.karimsammouri.resolve.dto.request.AddMessageRequest;
import com.karimsammouri.resolve.dto.request.CreateTicketRequest;
import com.karimsammouri.resolve.dto.response.ViewMessageResponse;
import com.karimsammouri.resolve.dto.response.ViewTicketResponse;
import com.karimsammouri.resolve.entity.*;
import com.karimsammouri.resolve.enums.MessageSenderType;
import com.karimsammouri.resolve.enums.TicketStatus;
import com.karimsammouri.resolve.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PublicTicketServiceTest {
    private final TokenService tokenService = mock(TokenService.class);
    private final EmailService emailService = mock(EmailService.class);

    private final OrganizationRepository organizationRepo = mock(OrganizationRepository.class);
    private final TicketRepository ticketRepo = mock(TicketRepository.class);
    private final TicketStatusHistoryRepository ticketStatusHistoryRepo = mock(TicketStatusHistoryRepository.class);
    private final MessageRepository messageRepo = mock(MessageRepository.class);
    private final CustomerTicketAccessRepository ticketAccessRepo = mock(CustomerTicketAccessRepository.class);

    private final PublicTicketService publicTicketService = new PublicTicketService(
            "http://localhost:8080",
            tokenService,
            emailService,
            organizationRepo,
            ticketRepo,
            ticketStatusHistoryRepo,
            messageRepo,
            ticketAccessRepo);

    private Long ticketId;
    private String token;
    private String tokenHash;

    private Organization organization;
    private Ticket ticket;

    @BeforeEach
    void setup() {
        ticketId = 123L;
        token = "customer-token";
        tokenHash = "hashed-token";

        organization = new Organization("Acme", "acme");
        ticket = new Ticket("Cannot login",
                "John Smith",
                "johnsmith@acme.com",
                organization);

        when(tokenService.generateToken()).thenReturn(token);
        when(tokenService.hashToken(token)).thenReturn(tokenHash);
        when(ticketAccessRepo.findByTicket_IdAndTokenHashAndRevokedAtIsNull(ticketId, tokenHash))
                .thenReturn(Optional.of(mock(CustomerTicketAccess.class)));
        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(ticket));
    }

    @Test
    void createTicket_shouldCreateTicketAndScheduleConfirmationEmail() {
        String organizationSlug = "acme";
        CreateTicketRequest request = new CreateTicketRequest(
                "John Smith",
                "johnsmith@acme.com",
                "Cannot login",
                "I cannot login");

        when(organizationRepo.findBySlug(organizationSlug)).thenReturn(Optional.of(organization));

        // Simulate database assigning a ticket ID when ticket is saved
        doAnswer(invocation -> {
            Ticket savedTicket = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedTicket, "id", ticketId);
            return savedTicket;
        }).when(ticketRepo).save(any(Ticket.class));

        TransactionSynchronizationManager.initSynchronization();

        try {
            Long generatedTicketId = publicTicketService.createTicket(organizationSlug, request);
            assertEquals(ticketId, generatedTicketId);

            ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepo).save(ticketCaptor.capture());

            Ticket savedTicket = ticketCaptor.getValue();
            assertEquals("Cannot login", savedTicket.getSubject());
            assertEquals("John Smith", savedTicket.getCustomerName());
            assertEquals("johnsmith@acme.com", savedTicket.getCustomerEmail());

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepo).save(messageCaptor.capture());

            Message initialMessage = messageCaptor.getValue();
            assertEquals(MessageSenderType.CUSTOMER, initialMessage.getSenderType());
            assertEquals("John Smith", initialMessage.getSenderName());
            assertEquals("I cannot login", initialMessage.getBody());

            verify(tokenService).generateToken();
            verify(tokenService).hashToken(token);
            verify(ticketAccessRepo).save(any(CustomerTicketAccess.class));

            // Email must not be sent before the transaction commits
            verifyNoInteractions(emailService);

            // Simulate transaction commit
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            verify(emailService).sendTicketConfirmation(
                    "johnsmith@acme.com",
                    "John Smith",
                    ticketId,
                    "Cannot login",
                    "http://localhost:8080/api/public/tickets/123?token=" + token);

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createTicket_shouldRejectInvalidOrganization() {
        String organizationSlug = "acme";
        CreateTicketRequest request = new CreateTicketRequest(
                "John Smith",
                "johnsmith@acme.com",
                "Cannot login",
                "I cannot login");

        when(organizationRepo.findBySlug(organizationSlug))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> publicTicketService.createTicket(organizationSlug, request));

        verify(ticketRepo, never()).save(any(Ticket.class));
        verify(messageRepo, never()).save(any(Message.class));
        verify(ticketAccessRepo, never()).save(any(CustomerTicketAccess.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void viewTicket_shouldReturnTicketAndMessages() {
        Message message = new Message(
                ticket,
                MessageSenderType.CUSTOMER,
                null,
                "John Smith",
                "I cannot login",
                false);

        when(messageRepo.findByTicket_IdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticketId))
                .thenReturn(List.of(message));

        ViewTicketResponse response = publicTicketService.viewTicket(ticketId, token);

        assertEquals(ticketId, response.ticketId());
        assertEquals("Cannot login", response.subject());
        assertEquals("OPEN", response.status());
        assertEquals("John Smith", response.customerName());
        assertEquals("johnsmith@acme.com", response.customerEmail());

        assertEquals(1, response.messages().size());

        ViewMessageResponse returnedMessage = response.messages().get(0);

        assertEquals("CUSTOMER", returnedMessage.senderType());
        assertEquals("John Smith", returnedMessage.senderName());
        assertEquals("I cannot login", returnedMessage.body());
    }

    @Test
    void viewTicket_shouldRejectInvalidOrRevokedToken() {
        when(ticketAccessRepo.findByTicket_IdAndTokenHashAndRevokedAtIsNull(ticketId, tokenHash))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> publicTicketService.viewTicket(ticketId, token));

        verify(messageRepo, never()).findByTicket_IdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticketId);
        verify(ticketRepo, never()).findById(ticketId);
    }

    @Test
    void addMessage_shouldSaveMessageAndMarkTicketAsUnread() {
        LocalDateTime previousUpdatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        ticket.setUpdatedAt(previousUpdatedAt);
        ticket.setHasUnreadCustomerMessage(false);

        AddMessageRequest request = new AddMessageRequest("I cannot login.");
        publicTicketService.addMessage(ticketId, token, request);

        assertTrue(ticket.hasUnreadCustomerMessage());
        assertTrue(ticket.getUpdatedAt().isAfter(previousUpdatedAt));

        verify(messageRepo).save(any(Message.class));
    }

    @Test
    void addMessage_shouldReopenResolvedTicket() {
        LocalDateTime previousUpdatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        ticket.setUpdatedAt(previousUpdatedAt);
        ticket.setHasUnreadCustomerMessage(false);

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());

        AddMessageRequest request = new AddMessageRequest("I cannot login.");
        publicTicketService.addMessage(ticketId, token, request);

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertNull(ticket.getResolvedAt());

        verify(ticketStatusHistoryRepo).save(any(TicketStatusHistory.class));

        assertTrue(ticket.hasUnreadCustomerMessage());
        assertTrue(ticket.getUpdatedAt().isAfter(previousUpdatedAt));

        verify(messageRepo).save(any(Message.class));
    }

    @Test
    void addMessage_shouldRejectMessagesOnArchivedTicket() {
        LocalDateTime previousUpdatedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        ticket.setUpdatedAt(previousUpdatedAt);
        ticket.setHasUnreadCustomerMessage(false);

        ticket.setStatus(TicketStatus.ARCHIVED);

        AddMessageRequest request = new AddMessageRequest("I cannot login.");
        assertThrows(IllegalStateException.class, () -> publicTicketService.addMessage(ticketId, token, request));

        assertEquals(TicketStatus.ARCHIVED, ticket.getStatus());
        assertFalse(ticket.hasUnreadCustomerMessage());
        assertEquals(previousUpdatedAt, ticket.getUpdatedAt());

        verify(ticketStatusHistoryRepo, never()).save(any(TicketStatusHistory.class));
        verify(messageRepo, never()).save(any(Message.class));
    }
}

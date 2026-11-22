package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.config.SecurityConfig;
import com.karimsammouri.resolve.config.SpringDataWebConfig;
import com.karimsammouri.resolve.dto.request.AgentMessageRequest;
import com.karimsammouri.resolve.dto.response.AgentMessageResponse;
import com.karimsammouri.resolve.dto.response.AgentTicketResponse;
import com.karimsammouri.resolve.dto.response.AgentTicketSummaryResponse;
import com.karimsammouri.resolve.enums.MessageSenderType;
import com.karimsammouri.resolve.enums.TicketStatus;
import com.karimsammouri.resolve.security.ResolveUserDetails;
import com.karimsammouri.resolve.service.AgentTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnableWebSecurity
@WebMvcTest(AgentTicketController.class)
@Import({SecurityConfig.class, SpringDataWebConfig.class})
public class AgentTicketControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AgentTicketService agentTicketService;

    private Long userId;
    private Long organizationId;
    private ResolveUserDetails principal;

    @BeforeEach
    void setup() {
        userId = 1L;
        organizationId = 10L;
        principal = mock(ResolveUserDetails.class);

        when(principal.getId()).thenReturn(userId);
        when(principal.getName()).thenReturn("Agent Smith");
        when(principal.getOrganizationId()).thenReturn(organizationId);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_AGENT"))).when(principal).getAuthorities();
    }

    @Test
    void viewTicket_withAuthentication_shouldReturnTicket() throws Exception {
        AgentMessageResponse messageResponse = new AgentMessageResponse(
                1L,
                MessageSenderType.CUSTOMER,
                "John Doe",
                "I cannot log in",
                false,
                LocalDateTime.of(2026, 11, 7, 10, 0));

        AgentTicketResponse ticketResponse = new AgentTicketResponse(
                123L,
                5L,
                "Unable to log in",
                TicketStatus.OPEN,
                "John Doe",
                "john@example.com",
                null,
                LocalDateTime.of(2026, 11, 7, 10, 0),
                LocalDateTime.of(2026, 11, 7, 10, 0),
                List.of(messageResponse));

        when(agentTicketService.viewTicket(123L, organizationId)).thenReturn(ticketResponse);

        mockMvc.perform(get("/api/agent/tickets/123")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketId").value(123))
                .andExpect(jsonPath("$.subject").value("Unable to log in"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].body").value("I cannot log in"));

        verify(agentTicketService).viewTicket(123L, organizationId);
    }

    @Test
    void viewTicket_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/agent/tickets/123"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(agentTicketService);
    }

    @Test
    void viewTickets_withPagination_shouldReturnPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        Page<AgentTicketSummaryResponse> page = new PageImpl<>(List.of(), pageable, 0);

        when(agentTicketService.viewTickets(organizationId, pageable)).thenReturn(page);

        mockMvc.perform(get("/api/agent/tickets")
                        .with(user(principal))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());

        verify(agentTicketService).viewTickets(organizationId, pageable);
    }

    @Test
    void addMessage_withValidRequest_shouldReturnCreated() throws Exception {
        AgentMessageRequest request = new AgentMessageRequest(
                "Please try resetting your password.", false);

        AgentMessageResponse response = new AgentMessageResponse(
                2L,
                MessageSenderType.STAFF,
                "Agent Smith",
                "Please try resetting your password.",
                false,
                LocalDateTime.now());

        when(agentTicketService.addMessage(123L, userId, organizationId, "Agent Smith", request))
                .thenReturn(response);

        mockMvc.perform(post("/api/agent/tickets/123/messages")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messageId").value(2))
                .andExpect(jsonPath("$.senderType").value("STAFF"))
                .andExpect(jsonPath("$.senderName").value("Agent Smith"))
                .andExpect(jsonPath("$.body").value("Please try resetting your password."))
                .andExpect(jsonPath("$.isInternalNote").value(false));

        verify(agentTicketService).addMessage(123L, userId, organizationId, "Agent Smith", request);
    }

    @Test
    void addMessage_withBlankBody_shouldReturnBadRequest() throws Exception {
        AgentMessageRequest request = new AgentMessageRequest("", false);

        mockMvc.perform(post("/api/agent/tickets/123/messages")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(containsString("body")));

        verifyNoInteractions(agentTicketService);
    }

    @Test
    void changeStatus_withValidRequest_shouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/api/agent/tickets/123/status")
                        .with(user(principal))
                        .with(csrf())
                        .param("targetStatus", "RESOLVED"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(agentTicketService).changeStatus(123L, userId, organizationId, TicketStatus.RESOLVED);
    }

    @Test
    void changeStatus_withInvalidStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/api/agent/tickets/123/status")
                        .with(user(principal))
                        .with(csrf())
                        .param("targetStatus", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message")
                        .value("Invalid value for parameter: targetStatus"));

        verifyNoInteractions(agentTicketService);
    }

    @Test
    void assignAgent_withValidAgentId_shouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/api/agent/tickets/123/assignedAgent")
                        .with(user(principal))
                        .with(csrf())
                        .param("targetAgentId", "456"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(agentTicketService).assignAgent(123L, organizationId, 456L);
    }

    @Test
    void assignAgent_withoutAgentId_shouldUnassignAgent() throws Exception {
        mockMvc.perform(patch("/api/agent/tickets/123/assignedAgent")
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(agentTicketService).assignAgent(123L, organizationId, null);
    }

    @Test
    void markAsRead_shouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/api/agent/tickets/123/read")
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(agentTicketService).markAsRead(123L, organizationId);
    }
}

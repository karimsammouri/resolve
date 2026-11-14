package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.config.SecurityConfig;
import com.karimsammouri.resolve.dto.request.AgentMessageRequest;
import com.karimsammouri.resolve.dto.response.AgentMessageResponse;
import com.karimsammouri.resolve.dto.response.AgentTicketResponse;
import com.karimsammouri.resolve.dto.response.AgentTicketSummaryResponse;
import com.karimsammouri.resolve.entity.Organization;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.enums.TicketStatus;
import com.karimsammouri.resolve.enums.UserRole;
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
@Import(SecurityConfig.class)
public class AgentTicketControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AgentTicketService agentTicketService;

    private User user;
    private ResolveUserDetails principal;

    @BeforeEach
    void setup() {
        user = new User(
                "Agent Smith",
                "agent@acme.com",
                "hashed-password",
                UserRole.AGENT,
                mock(Organization.class));

        principal = new ResolveUserDetails(user);
    }

    @Test
    void viewTicket_withAuthentication_shouldReturnTicket() throws Exception {
        AgentMessageResponse messageResponse = new AgentMessageResponse(
                1L,
                "CUSTOMER",
                "John Doe",
                "I cannot log in",
                false,
                LocalDateTime.of(2026, 11, 7, 10, 0));

        AgentTicketResponse ticketResponse = new AgentTicketResponse(
                123L,
                5L,
                "Unable to log in",
                "OPEN",
                "John Doe",
                "john@example.com",
                null,
                LocalDateTime.of(2026, 11, 7, 10, 0),
                LocalDateTime.of(2026, 11, 7, 10, 0),
                List.of(messageResponse));

        when(agentTicketService.viewTicket(123L, user))
                .thenReturn(ticketResponse);

        mockMvc.perform(get("/api/agent/tickets/123")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketId").value(123))
                .andExpect(jsonPath("$.subject").value("Unable to log in"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].body").value("I cannot log in"));

        verify(agentTicketService).viewTicket(123L, user);
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

        when(agentTicketService.viewTickets(user, pageable)).thenReturn(page);

        mockMvc.perform(get("/api/agent/tickets")
                        .with(user(principal))
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());

        verify(agentTicketService).viewTickets(user, pageable);
    }

    @Test
    void addMessage_withValidRequest_shouldReturnCreated() throws Exception {
        AgentMessageRequest request = new AgentMessageRequest(
                "Please try resetting your password.",
                false);

        when(agentTicketService.addMessage(123L, user, request))
                .thenReturn(2L);

        mockMvc.perform(post("/api/agent/tickets/123/messages")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("2"));

        verify(agentTicketService).addMessage(123L, user, request);
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
    void changeStatus_withValidRequest_shouldReturnOk() throws Exception {
        when(agentTicketService.changeStatus(123L, user, TicketStatus.RESOLVED))
                .thenReturn(123L);

        mockMvc.perform(patch("/api/agent/tickets/123/status")
                        .with(user(principal))
                        .with(csrf())
                        .param("targetStatus", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("123"));

        verify(agentTicketService).changeStatus(123L, user, TicketStatus.RESOLVED);
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
    void assignAgent_withValidAgentId_shouldReturnTicketId() throws Exception {
        when(agentTicketService.assignAgent(123L, user, 456L))
                .thenReturn(123L);

        mockMvc.perform(patch("/api/agent/tickets/123/assignedAgent")
                        .with(user(principal))
                        .with(csrf())
                        .param("targetAgentId", "456"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("123"));

        verify(agentTicketService).assignAgent(123L, user, 456L);
    }

    @Test
    void assignAgent_withoutAgentId_shouldUnassignAgent() throws Exception {
        when(agentTicketService.assignAgent(123L, user, null))
                .thenReturn(123L);

        mockMvc.perform(patch("/api/agent/tickets/123/assignedAgent")
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("123"));

        verify(agentTicketService).assignAgent(123L, user, null);
    }
}

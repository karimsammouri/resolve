package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.request.AddMessageRequest;
import com.karimsammouri.resolve.dto.request.CreateTicketRequest;
import com.karimsammouri.resolve.dto.response.ViewMessageResponse;
import com.karimsammouri.resolve.dto.response.ViewTicketResponse;
import com.karimsammouri.resolve.exception.InvalidTicketAccessException;
import com.karimsammouri.resolve.service.PublicTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicTicketController.class)
public class PublicTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PublicTicketService publicTicketService;

    @Test
    void createTicket_withValidRequest_shouldReturnCreated() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                "John Doe",
                "john@example.com",
                "Unable to log in",
                "I cannot access my account"
        );

        when(publicTicketService.createTicket("acme", request))
                .thenReturn(123L);

        mockMvc.perform(post("/api/public/organizations/acme/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("123"));

        verify(publicTicketService).createTicket("acme", request);
    }

    @Test
    void createTicket_withBlankCustomerName_shouldReturnBadRequest() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                "",
                "john@example.com",
                "Unable to log in",
                "I cannot access my account"
        );

        mockMvc.perform(post("/api/public/organizations/acme/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("customerName")));

        verifyNoInteractions(publicTicketService);
    }

    @Test
    void viewTicket_withValidToken_shouldReturnTicket() throws Exception {
        ViewMessageResponse messageResponse1 = new ViewMessageResponse(
                1L,
                "CUSTOMER",
                "John Doe",
                "I cannot log in",
                LocalDateTime.of(2026, 11, 7, 10, 0));

        ViewTicketResponse ticketResponse = new ViewTicketResponse(
                123L,
                "Unable to log in",
                "OPEN",
                "John Doe",
                "john@example.com",
                List.of(messageResponse1));

        when(publicTicketService.viewTicket(123L, "valid-token"))
                .thenReturn(ticketResponse);

        mockMvc.perform(get("/api/public/tickets/123")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketId").value(123))
                .andExpect(jsonPath("$.subject").value("Unable to log in"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].body").value("I cannot log in"));

        verify(publicTicketService).viewTicket(123L, "valid-token");
    }

    @Test
    void viewTicket_withInvalidToken_shouldReturnForbidden() throws Exception {
        when(publicTicketService.viewTicket(123L, "invalid-token"))
                .thenThrow(new InvalidTicketAccessException("Invalid access token"));

        mockMvc.perform(get("/api/public/tickets/123")
                        .param("token", "invalid-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid access token"));

        verify(publicTicketService).viewTicket(123L, "invalid-token");
    }

    @Test
    void addMessage_withValidRequest_shouldReturnCreated() throws Exception {
        AddMessageRequest request = new AddMessageRequest("I still cannot log in");

        when(publicTicketService.addMessage(123L, "valid-token", request)).thenReturn(1L);

        mockMvc.perform(post("/api/public/tickets/123/messages")
                        .param("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("1"));

        verify(publicTicketService).addMessage(123L, "valid-token", request);
    }

    @Test
    void addMessage_withBlankBody_shouldReturnBadRequest() throws Exception {
        AddMessageRequest request = new AddMessageRequest("");

        mockMvc.perform(post("/api/public/tickets/123/messages")
                        .param("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("body")));

        verifyNoInteractions(publicTicketService);
    }
}

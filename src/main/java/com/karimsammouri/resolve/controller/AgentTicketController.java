package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.request.AgentMessageRequest;
import com.karimsammouri.resolve.dto.response.AgentTicketResponse;
import com.karimsammouri.resolve.dto.response.AgentTicketSummaryResponse;
import com.karimsammouri.resolve.enums.TicketStatus;
import com.karimsammouri.resolve.security.ResolveUserDetails;
import com.karimsammouri.resolve.service.AgentTicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentTicketController {
    private final AgentTicketService agentTicketService;

    public AgentTicketController(AgentTicketService agentTicketService) {
        this.agentTicketService = agentTicketService;
    }

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<AgentTicketResponse> viewTicket(
            @PathVariable Long ticketId, @AuthenticationPrincipal ResolveUserDetails principal) {
        AgentTicketResponse response = agentTicketService.viewTicket(ticketId, principal.getUser());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tickets")
    public ResponseEntity<Page<AgentTicketSummaryResponse>> viewTickets(
            @AuthenticationPrincipal ResolveUserDetails principal, Pageable pageable) {
        Page<AgentTicketSummaryResponse> response = agentTicketService.viewTickets(principal.getUser(), pageable);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<Long> addMessage(@PathVariable Long ticketId,
                                           @AuthenticationPrincipal ResolveUserDetails principal,
                                           @Valid @RequestBody AgentMessageRequest request) {
        Long messageId = agentTicketService.addMessage(ticketId, principal.getUser(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageId);
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public ResponseEntity<Long> changeStatus(@PathVariable Long ticketId,
                                             @AuthenticationPrincipal ResolveUserDetails principal,
                                             @RequestParam TicketStatus targetStatus) {
        Long updatedTicketId = agentTicketService.changeStatus(ticketId, principal.getUser(), targetStatus);

        return ResponseEntity.ok(updatedTicketId);
    }

    @PatchMapping("/tickets/{ticketId}/assignedAgent")
    public ResponseEntity<Long> assignAgent(@PathVariable Long ticketId,
                                            @AuthenticationPrincipal ResolveUserDetails principal,
                                            @RequestParam(required = false) Long targetAgentId) {
        Long updatedTicketId = agentTicketService.assignAgent(ticketId, principal.getUser(), targetAgentId);

        return ResponseEntity.ok(updatedTicketId);
    }
}

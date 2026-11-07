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
    public AgentTicketResponse viewTicket(@PathVariable Long ticketId,
                                          @AuthenticationPrincipal ResolveUserDetails principal) {
        return agentTicketService.viewTicket(ticketId, principal.getUser());
    }

    @GetMapping("/tickets")
    public Page<AgentTicketSummaryResponse> viewTickets(@AuthenticationPrincipal ResolveUserDetails principal,
                                                        Pageable pageable) {
        return agentTicketService.viewTickets(principal.getUser(), pageable);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public Long addMessage(@PathVariable Long ticketId,
                           @AuthenticationPrincipal ResolveUserDetails principal,
                           @Valid @RequestBody AgentMessageRequest request) {
        return agentTicketService.addMessage(ticketId, principal.getUser(), request);
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public Long changeStatus(@PathVariable Long ticketId,
                             @AuthenticationPrincipal ResolveUserDetails principal,
                             @RequestParam TicketStatus targetStatus) {
        return agentTicketService.changeStatus(ticketId, principal.getUser(), targetStatus);
    }

    @PatchMapping("/tickets/{ticketId}/assignedAgent")
    public Long assignAgent(@PathVariable Long ticketId,
                            @AuthenticationPrincipal ResolveUserDetails principal,
                            @RequestParam(required = false) Long targetAgentId) {
        return agentTicketService.assignAgent(ticketId, principal.getUser(), targetAgentId);
    }
}

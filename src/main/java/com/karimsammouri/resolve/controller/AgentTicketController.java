package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.AgentMessageRequest;
import com.karimsammouri.resolve.dto.AgentTicketResponse;
import com.karimsammouri.resolve.dto.AgentTicketSummaryResponse;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.enums.TicketStatus;
import com.karimsammouri.resolve.repository.UserRepository;
import com.karimsammouri.resolve.service.AgentTicketService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentTicketController {
    private final AgentTicketService agentTicketService;
    private final UserRepository userRepository;

    public AgentTicketController(AgentTicketService agentTicketService, UserRepository userRepository) {
        this.agentTicketService = agentTicketService;
        this.userRepository = userRepository;
    }

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<AgentTicketResponse> viewTicket(@PathVariable Long ticketId,
                                                          @RequestParam Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        AgentTicketResponse response = agentTicketService.viewTicket(ticketId, user);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tickets")
    public ResponseEntity<Page<AgentTicketSummaryResponse>> viewTickets(@RequestParam Long userId,
                                                                        Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Page<AgentTicketSummaryResponse> response = agentTicketService.viewTickets(user, pageable);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<Long> addMessage(@PathVariable Long ticketId,
                                           @RequestParam Long userId,
                                           @Valid @RequestBody AgentMessageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Long messageId = agentTicketService.addMessage(ticketId, user, request.body(), request.isInternalNote());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageId);
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public ResponseEntity<Long> changeStatus(@PathVariable Long ticketId,
                                             @RequestParam Long userId,
                                             @RequestParam TicketStatus targetStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Long updatedTicketId = agentTicketService.changeStatus(ticketId, user, targetStatus);

        return ResponseEntity.ok(updatedTicketId);
    }

    @PatchMapping("/tickets/{ticketId}/assignedAgent")
    public ResponseEntity<Long> assignAgent(@PathVariable Long ticketId,
                                            @RequestParam Long userId,
                                            @RequestParam(required = false) Long assignedAgentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Long updatedTicketId = agentTicketService.assignAgent(ticketId, user, assignedAgentId);

        return ResponseEntity.ok(updatedTicketId);
    }
}

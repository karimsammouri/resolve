package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.AgentTicketResponse;
import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.repository.UserRepository;
import com.karimsammouri.resolve.service.AgentTicketService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentTicketController {
    private final AgentTicketService agentTicketService;
    private final UserRepository userRepository;

    public AgentTicketController(AgentTicketService agentTicketService, UserRepository userRepository) {
        this.agentTicketService = agentTicketService;
        this.userRepository = userRepository;
    }

    @GetMapping("/api/agent/tickets/{ticketId}")
    public ResponseEntity<AgentTicketResponse> viewTicket(@PathVariable Long ticketId,
                                                          @RequestParam Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        AgentTicketResponse response = agentTicketService.viewTicket(ticketId, user);

        return ResponseEntity.ok(response);
    }
}

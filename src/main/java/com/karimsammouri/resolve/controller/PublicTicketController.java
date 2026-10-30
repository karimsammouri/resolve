package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.AddMessageRequest;
import com.karimsammouri.resolve.dto.CreateTicketRequest;
import com.karimsammouri.resolve.dto.ViewTicketResponse;
import com.karimsammouri.resolve.service.PublicTicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublicTicketController {
    private final PublicTicketService ticketService;

    public PublicTicketController(PublicTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/api/public/organizations/{slug}/tickets")
    public ResponseEntity<Long> createTicket(@PathVariable("slug") String organizationSlug,
                                             @Valid @RequestBody CreateTicketRequest request) {
        Long ticketId = ticketService.createTicket(
                organizationSlug,
                request.customerName(),
                request.customerEmail(),
                request.messageSubject(),
                request.messageBody());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketId);
    }

    @GetMapping("/api/public/tickets/{ticketId}")
    public ViewTicketResponse viewTicket(@PathVariable Long ticketId,
                                         @RequestParam String token) {
        return ticketService.viewTicket(ticketId, token);
    }

    @PostMapping("/api/public/tickets/{ticketId}/messages")
    public ResponseEntity<Long> addMessage(@PathVariable Long ticketId,
                                           @RequestParam String token,
                                           @Valid @RequestBody AddMessageRequest request) {
        Long messageId = ticketService.addMessage(ticketId, token, request.body());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageId);
    }
}

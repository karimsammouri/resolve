package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.CreateTicketRequestDTO;
import com.karimsammouri.resolve.service.PublicTicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublicTicketController {
    private final PublicTicketService ticketService;

    public PublicTicketController(PublicTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/api/public/organizations/{slug}/tickets")
    public Long createTicket(@PathVariable("slug") String organizationSlug,
                             @Valid @RequestBody CreateTicketRequestDTO request) {
        return ticketService.createTicket(organizationSlug,
                request.customerName(),
                request.customerEmail(),
                request.messageSubject(),
                request.messageBody());
    }
}

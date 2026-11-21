package com.karimsammouri.resolve.controller;

import com.karimsammouri.resolve.dto.request.AddMessageRequest;
import com.karimsammouri.resolve.dto.request.CreateTicketRequest;
import com.karimsammouri.resolve.dto.response.CreateTicketResponse;
import com.karimsammouri.resolve.dto.response.ViewMessageResponse;
import com.karimsammouri.resolve.dto.response.ViewTicketResponse;
import com.karimsammouri.resolve.service.PublicTicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicTicketController {
    private final PublicTicketService publicTicketService;

    public PublicTicketController(PublicTicketService publicTicketService) {
        this.publicTicketService = publicTicketService;
    }

    @PostMapping("/organizations/{slug}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTicketResponse createTicket(@PathVariable("slug") String organizationSlug,
                                             @Valid @RequestBody CreateTicketRequest request) {
        return publicTicketService.createTicket(organizationSlug, request);
    }

    @GetMapping("/tickets/{ticketId}")
    public ViewTicketResponse viewTicket(@PathVariable Long ticketId, @RequestParam String token) {
        return publicTicketService.viewTicket(ticketId, token);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ViewMessageResponse addMessage(@PathVariable Long ticketId,
                                          @RequestParam String token,
                                          @Valid @RequestBody AddMessageRequest request) {
        return publicTicketService.addMessage(ticketId, token, request);
    }
}

package com.karimsammouri.resolve.entity;

import com.karimsammouri.resolve.enums.TicketStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class TicketStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TicketStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TicketStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by_user_id", nullable = false, updatable = false)
    private User updatedByUser;

    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected TicketStatusHistory() {
    }

    public TicketStatusHistory(Ticket ticket,
                               TicketStatus oldStatus,
                               TicketStatus newStatus,
                               User updatedByUser) {
        this.ticket = ticket;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.updatedByUser = updatedByUser;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public TicketStatus getOldStatus() {
        return oldStatus;
    }

    public TicketStatus getNewStatus() {
        return newStatus;
    }

    public User getUpdatedByUser() {
        return updatedByUser;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}

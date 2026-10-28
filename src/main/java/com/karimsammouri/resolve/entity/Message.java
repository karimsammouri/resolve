package com.karimsammouri.resolve.entity;

import com.karimsammouri.resolve.enums.MessageSenderType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MessageSenderType senderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", updatable = false)
    private User senderUser;

    @Column(nullable = false, updatable = false)
    private String senderName;

    @Column(nullable = false, updatable = false)
    private String body;

    @Column(nullable = false, updatable = false)
    private boolean isInternalNote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Message() {
    }

    public Message(Ticket ticket,
                   MessageSenderType senderType,
                   User senderUser,
                   String senderName,
                   String body,
                   boolean isInternalNote) {
        this.ticket = ticket;
        this.senderType = senderType;
        this.senderUser = senderUser;
        this.senderName = senderName;
        this.body = body;
        this.isInternalNote = isInternalNote;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public MessageSenderType getSenderType() {
        return senderType;
    }

    public User getSenderUser() {
        return senderUser;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getBody() {
        return body;
    }

    public boolean isInternalNote() {
        return isInternalNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

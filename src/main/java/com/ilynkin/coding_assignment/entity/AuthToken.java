package com.ilynkin.coding_assignment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "AUTH_TOKENS", indexes = {
        @Index(name = "IDX_AUTH_TOKENS_USER",
                columnList = "USER_ID"),
        @Index(name = "IDX_AUTH_TOKENS_TOKEN",
                columnList = "TOKEN")}, uniqueConstraints = {@UniqueConstraint(name = "CONSTRAINT_FD",
        columnNames = {"TOKEN"})})
public class AuthToken {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "ID", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Size(max = 255)
    @NotNull
    @Column(name = "TOKEN", nullable = false, length = 255)
    private String token;

    @NotNull
    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
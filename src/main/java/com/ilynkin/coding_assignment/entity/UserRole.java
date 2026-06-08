package com.ilynkin.coding_assignment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "USER_ROLES", uniqueConstraints = {@UniqueConstraint(name = "CONSTRAINT_C6",
        columnNames = {"CODE"})})
public class UserRole {
    @Id
    @Column(name = "ID", nullable = false)
    private Long id;

    @Size(max = 63)
    @NotNull
    @Column(name = "CODE", nullable = false, length = 63)
    private String code;
}
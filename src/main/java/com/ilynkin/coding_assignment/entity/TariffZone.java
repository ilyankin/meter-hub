package com.ilynkin.coding_assignment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "TARIFF_ZONES", uniqueConstraints = {@UniqueConstraint(name = "CONSTRAINT_61",
        columnNames = {"CODE"})})
public class TariffZone {
    @Id
    @Column(name = "ID", nullable = false)
    private Long id;

    @Size(max = 15)
    @NotNull
    @Column(name = "CODE", nullable = false, length = 15)
    private String code;

    @Size(max = 255)
    @Column(name = "DESCRIPTION")
    private String description;
}
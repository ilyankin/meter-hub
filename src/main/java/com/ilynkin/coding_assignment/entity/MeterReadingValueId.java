package com.ilynkin.coding_assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class MeterReadingValueId implements Serializable {
    @Serial
    private static final long serialVersionUID = 6627333627721615699L;

    @NotNull
    @Column(name = "READING_ID", nullable = false)
    private UUID readingId;

    @NotNull
    @Column(name = "TARIFF_ZONE_ID", nullable = false)
    private Long tariffZoneId;
}
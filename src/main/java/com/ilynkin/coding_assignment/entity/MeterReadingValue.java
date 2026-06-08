package com.ilynkin.coding_assignment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "METER_READING_VALUES", indexes = {@Index(name = "IDX_READING_VALUES_ZONE",
        columnList = "TARIFF_ZONE_ID")})
public class MeterReadingValue {
    @EmbeddedId
    private MeterReadingValueId id;

    @MapsId("readingId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "READING_ID", nullable = false)
    private MeterReading reading;

    @MapsId("tariffZoneId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TARIFF_ZONE_ID", nullable = false)
    private TariffZone tariffZone;

    @NotNull
    @Column(name = "READING_VALUE", nullable = false, precision = 12, scale = 3)
    private BigDecimal readingValue;
}
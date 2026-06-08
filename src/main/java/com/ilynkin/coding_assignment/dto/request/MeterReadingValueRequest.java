package com.ilynkin.coding_assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MeterReadingValueRequest(
        @NotBlank @Size(max = 15) String tariffZone,
        @NotNull @PositiveOrZero BigDecimal value
) {
}

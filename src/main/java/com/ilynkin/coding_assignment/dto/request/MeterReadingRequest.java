package com.ilynkin.coding_assignment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record MeterReadingRequest(
        @NotNull Long meterId,
        Instant readingDate,
        @NotEmpty @Valid List<MeterReadingValueRequest> values
) {
}

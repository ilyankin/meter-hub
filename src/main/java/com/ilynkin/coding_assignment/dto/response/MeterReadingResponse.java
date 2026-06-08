package com.ilynkin.coding_assignment.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeterReadingResponse(
        UUID id,
        Long meterId,
        Instant readingDate,
        List<MeterReadingValueResponse> values,
        Instant createdAt
) {
}

package com.ilynkin.coding_assignment.dto.response;

import java.math.BigDecimal;

public record MeterReadingValueResponse(
        String tariffZone,
        BigDecimal value
) {
}

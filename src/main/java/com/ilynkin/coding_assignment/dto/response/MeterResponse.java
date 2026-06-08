package com.ilynkin.coding_assignment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MeterResponse(
        Long id,
        Long userId,
        String serialNumber,
        String inventoryNumber,
        Integer manufactureYear,
        BigDecimal transformationRatio,
        LocalDate installationDate,
        String sealNumber,
        String antimagneticSealNumber,
        String installationLocation,
        String notes,
        String gisId,
        Instant createdAt,
        Instant updatedAt
) {
}

package com.ilynkin.coding_assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MeterRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 100) String serialNumber,
        @Size(max = 100) String inventoryNumber,
        Integer manufactureYear,
        @Positive BigDecimal transformationRatio,
        LocalDate installationDate,
        @Size(max = 100) String sealNumber,
        @Size(max = 100) String antimagneticSealNumber,
        @Size(max = 500) String installationLocation,
        String notes,
        @Size(max = 100) String gisId
) {
}

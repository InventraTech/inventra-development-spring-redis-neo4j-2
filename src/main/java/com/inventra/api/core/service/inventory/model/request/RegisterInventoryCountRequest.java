package com.inventra.api.core.service.inventory.model.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RegisterInventoryCountRequest(
        @NotNull
        Integer batchId,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 9, fraction = 3)
        BigDecimal physicalQuantity,

        @Size(max = 255)
        String note
) {
}

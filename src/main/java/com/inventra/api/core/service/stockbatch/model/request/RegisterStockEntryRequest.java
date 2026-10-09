package com.inventra.api.core.service.stockbatch.model.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RegisterStockEntryRequest(
        @NotNull
        Integer productId,

        @NotNull
        Integer kitchenId,

        Integer supplierId,

        @NotBlank
        @Size(max = 50)
        String batchNumber,

        @Size(max = 50)
        String invoiceNumber,

        @NotNull
        @Positive
        @Digits(integer = 9, fraction = 3)
        BigDecimal initialQuantity,

        @NotNull
        LocalDate entryDate,

        LocalDate expirationDate,

        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal unitPrice
) {
}

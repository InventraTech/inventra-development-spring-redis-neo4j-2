package com.inventra.api.core.service.stockbatch.model.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AdjustStockRequest(
        @NotNull
        @PositiveOrZero
        @Digits(integer = 9, fraction = 3)
        BigDecimal newQuantity
) {
}

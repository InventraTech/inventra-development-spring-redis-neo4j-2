package com.inventra.api.core.service.product.model.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SetKitchenParametersRequest(
        @NotNull
        Integer kitchenId,

        @PositiveOrZero
        @Digits(integer = 9, fraction = 3)
        BigDecimal minStock,

        @PositiveOrZero
        @Digits(integer = 9, fraction = 3)
        BigDecimal maxStock,

        @PositiveOrZero
        @Digits(integer = 9, fraction = 3)
        BigDecimal averageDailyConsumption
) {
}

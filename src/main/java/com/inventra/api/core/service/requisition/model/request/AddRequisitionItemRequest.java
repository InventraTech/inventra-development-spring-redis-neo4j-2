package com.inventra.api.core.service.requisition.model.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AddRequisitionItemRequest(
        @NotNull
        Integer productId,

        @NotNull
        @Positive
        @Digits(integer = 9, fraction = 3)
        BigDecimal quantity,

        Integer suggestedSupplierId,

        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal estimatedPrice,

        @Size(max = 255)
        String note
) {
}

package com.inventra.api.core.service.product.model.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LinkSupplierRequest(
        @NotNull
        Integer supplierId,

        @Size(max = 50)
        String supplierCode,

        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal referencePrice,

        @PositiveOrZero
        Integer leadTimeDays
) {
}

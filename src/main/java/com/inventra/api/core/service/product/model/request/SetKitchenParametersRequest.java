package com.inventra.api.core.service.product.model.request;

import java.math.BigDecimal;

public record SetKitchenParametersRequest(
        Integer kitchenId,
        BigDecimal minStock,
        BigDecimal maxStock,
        BigDecimal averageDailyConsumption
) {
}

package com.inventra.api.application.service.Product.model.request;

import java.math.BigDecimal;

public record SetKitchenParametersRequest(
        Integer kitchenId,
        BigDecimal minStock,
        BigDecimal maxStock,
        BigDecimal averageDailyConsumption
) {
}

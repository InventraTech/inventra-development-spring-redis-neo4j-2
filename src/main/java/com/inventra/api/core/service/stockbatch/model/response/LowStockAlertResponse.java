package com.inventra.api.core.service.stockbatch.model.response;

import java.math.BigDecimal;

public record LowStockAlertResponse(
        Integer kitchenId,
        Integer productId,
        BigDecimal currentQuantity,
        BigDecimal minStock
) {
}

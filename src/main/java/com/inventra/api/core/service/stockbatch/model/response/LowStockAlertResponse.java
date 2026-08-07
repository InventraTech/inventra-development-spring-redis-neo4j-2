package com.inventra.api.core.service.stockbatch.model.response;

import java.math.BigDecimal;

public record LowStockAlertResponse(
        Integer kitchenId,
        Long productId,
        BigDecimal currentQuantity,
        BigDecimal minStock
) {
}

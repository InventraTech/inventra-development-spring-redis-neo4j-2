package com.inventra.api.application.service.StockBatch.model.response;

import java.math.BigDecimal;

public record LowStockAlertResponse(
        Integer kitchenId,
        Long productId,
        BigDecimal currentQuantity,
        BigDecimal minStock
) {
}

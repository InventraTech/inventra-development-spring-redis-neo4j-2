package com.inventra.api.application.service.StockBatch.model.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterStockEntryRequest(
        Long productId,
        Integer kitchenId,
        Integer supplierId,
        String batchNumber,
        String invoiceNumber,
        BigDecimal initialQuantity,
        LocalDate entryDate,
        LocalDate expirationDate,
        BigDecimal unitPrice
) {
}

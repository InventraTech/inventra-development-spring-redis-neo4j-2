package com.inventra.api.core.service.stockbatch.model.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterStockEntryRequest(
        Integer productId,
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

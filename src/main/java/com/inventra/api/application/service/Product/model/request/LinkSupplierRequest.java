package com.inventra.api.application.service.Product.model.request;

import java.math.BigDecimal;

public record LinkSupplierRequest(
        Integer supplierId,
        String supplierCode,
        BigDecimal referencePrice,
        Integer leadTimeDays
) {
}

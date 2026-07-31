package com.inventra.api.core.service.product.model.request;

import java.math.BigDecimal;

public record LinkSupplierRequest(
        Integer supplierId,
        String supplierCode,
        BigDecimal referencePrice,
        Integer leadTimeDays
) {
}

package com.inventra.api.core.service.requisition.model.request;

import java.math.BigDecimal;

public record AddRequisitionItemRequest(
        Long productId,
        BigDecimal quantity,
        Integer suggestedSupplierId,
        BigDecimal estimatedPrice,
        String note
) {
}

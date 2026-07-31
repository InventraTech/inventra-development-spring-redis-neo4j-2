package com.inventra.api.application.service.Requisition.model.request;

import java.util.UUID;

import com.inventra.api.core.domain.requisition.enums.RequisitionType;

public record CreateRequisitionRequest(
        RequisitionType type,
        String origin,
        Integer kitchenId,
        UUID requesterId
) {
}

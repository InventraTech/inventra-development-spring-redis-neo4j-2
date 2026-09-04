package com.inventra.api.core.service.requisition.model.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ApproveRequisitionRequest(
        @NotNull
        UUID approverId
) {
}

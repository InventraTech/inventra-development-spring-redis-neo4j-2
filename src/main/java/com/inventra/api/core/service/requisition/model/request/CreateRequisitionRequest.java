package com.inventra.api.core.service.requisition.model.request;

import com.inventra.api.core.domain.requisition.enums.RequisitionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// O solicitante é o usuário logado (não vem do body).
public record CreateRequisitionRequest(
        @NotNull
        RequisitionType type,

        @NotBlank
        @Size(max = 20)
        String origin,

        @NotNull
        Integer kitchenId
) {
}

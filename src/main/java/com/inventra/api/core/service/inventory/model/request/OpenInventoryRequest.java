package com.inventra.api.core.service.inventory.model.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OpenInventoryRequest(
        @NotNull
        Integer kitchenId,

        @NotNull
        UUID responsibleId,

        @Size(max = 255)
        String note
) {
}

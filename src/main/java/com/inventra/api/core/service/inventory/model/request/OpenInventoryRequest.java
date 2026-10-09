package com.inventra.api.core.service.inventory.model.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// responsibleId é opcional: sem ele, o responsável é o usuário logado.
public record OpenInventoryRequest(
        @NotNull
        Integer kitchenId,

        UUID responsibleId,

        @Size(max = 255)
        String note
) {
}

package com.inventra.api.core.service.kitchen.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKitchenRequest(
        @Schema(description = "Nome da cozinha", example = "Cozinha Central", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 120)
        String name,

        @Schema(description = "Endereço", example = "Rua das Flores, 100")
        @Size(max = 255)
        String address
) {
}

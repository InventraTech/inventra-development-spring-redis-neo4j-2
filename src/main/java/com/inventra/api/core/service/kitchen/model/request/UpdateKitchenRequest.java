package com.inventra.api.core.service.kitchen.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateKitchenRequest(
        @Schema(description = "Nome da cozinha", example = "Cozinha Central")
        @Size(max = 120)
        @Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
        String name,

        @Schema(description = "Endereço", example = "Rua das Flores, 100")
        @Size(max = 255)
        String address
) {
}

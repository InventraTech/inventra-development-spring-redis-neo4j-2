package com.inventra.api.core.service.kitchenaccess.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKitchenAccessRequest(
        @Schema(description = "Código de convite da cozinha", example = "AB12CD34", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 20)
        String code
) {
}

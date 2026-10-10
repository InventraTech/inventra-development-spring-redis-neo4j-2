package com.inventra.api.core.service.user.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(description = "Senha atual", example = "Senha@123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String currentPassword,

        @Schema(description = "Nova senha (mínimo 8 caracteres)", example = "NovaSenha@456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword
) {
}

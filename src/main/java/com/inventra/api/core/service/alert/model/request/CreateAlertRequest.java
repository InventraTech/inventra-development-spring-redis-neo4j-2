package com.inventra.api.core.service.alert.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.inventra.api.core.domain.alert.enums.AlertSeverity;

public record CreateAlertRequest(
        @NotBlank
        @Size(max = 30)
        String type,

        @NotNull
        AlertSeverity severity,

        Integer batchId,

        Integer productId,

        @NotNull
        Integer kitchenId,

        @NotBlank
        @Size(max = 255)
        String message
) {
}

package com.inventra.api.core.service.unit.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUnitRequest(
        @NotBlank
        @Size(max = 10)
        String symbol,

        @NotBlank
        @Size(max = 60)
        String description
) {
}

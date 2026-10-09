package com.inventra.api.core.service.unit.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUnitRequest(
        @Size(max = 10)
        @Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
        String symbol,

        @Size(max = 60)
        String description
) {
}

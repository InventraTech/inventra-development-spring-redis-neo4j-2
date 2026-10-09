package com.inventra.api.core.service.kitchen.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKitchenRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 255)
        String address
) {
}

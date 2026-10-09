package com.inventra.api.core.service.kitchen.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateKitchenRequest(
        @Size(max = 120)
        @Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
        String name,

        @Size(max = 255)
        String address
) {
}

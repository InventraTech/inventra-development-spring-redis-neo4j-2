package com.inventra.api.core.service.kitchenaccess.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKitchenAccessRequest(
        @NotBlank
        @Size(max = 20)
        String code
) {
}

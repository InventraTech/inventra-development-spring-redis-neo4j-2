package com.inventra.api.core.service.user.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 120)
        @Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
        String name,

        Integer kitchenId,

        Integer profileId
) {
}

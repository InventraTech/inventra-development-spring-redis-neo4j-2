package com.inventra.api.core.service.profile.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 50)
        @Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
        String accessType,

        @Size(max = 255)
        String description
) {
}

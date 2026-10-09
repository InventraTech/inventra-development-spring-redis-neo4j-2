package com.inventra.api.core.service.profile.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProfileRequest(
        @NotBlank
        @Size(max = 50)
        String accessType,

        @Size(max = 255)
        String description
) {
}

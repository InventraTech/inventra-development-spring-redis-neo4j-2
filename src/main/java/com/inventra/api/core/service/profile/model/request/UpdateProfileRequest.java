package com.inventra.api.core.service.profile.model.request;

public record UpdateProfileRequest(
        String accessType,
        String description
) {
}

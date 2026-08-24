package com.inventra.api.core.service.profile.model.request;

public record CreateProfileRequest(
        String accessType,
        String description
) {
}

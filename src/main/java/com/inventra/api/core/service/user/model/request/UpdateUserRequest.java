package com.inventra.api.core.service.user.model.request;

public record UpdateUserRequest(
        String name,
        String role,
        Integer kitchenId,
        Integer profileId
) {
}

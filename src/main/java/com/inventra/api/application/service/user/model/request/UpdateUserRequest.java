package com.inventra.api.application.service.user.model.request;

public record UpdateUserRequest(
        String name,
        String role,
        Integer kitchenId,
        Integer profileId
) {
}

package com.inventra.api.core.service.user.model.request;

public record CreateUserRequest(
        String name,
        String email,
        String password,
        String role,
        Integer kitchenId,
        Integer profileId
) {
}

package com.inventra.api.core.service.user.model.request;

public record CreateUserRequest(
        String name,
        String email,
        String password,
        Integer kitchenId,
        Integer profileId
) {
}

package com.inventra.api.core.service.user.model.request;

public record UpdateUserRequest(
        String name,
        Integer kitchenId,
        Integer profileId
) {
}

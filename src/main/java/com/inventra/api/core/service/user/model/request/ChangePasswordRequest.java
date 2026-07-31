package com.inventra.api.core.service.user.model.request;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}

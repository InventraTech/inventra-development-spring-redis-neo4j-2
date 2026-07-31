package com.inventra.api.application.service.user.model.request;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}

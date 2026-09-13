package com.inventra.api.core.service.auth.model.response;

import com.inventra.api.core.service.user.model.response.UserResponse;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}

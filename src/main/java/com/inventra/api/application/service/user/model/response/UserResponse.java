package com.inventra.api.application.service.user.model.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.inventra.api.core.domain.user.User;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String role,
        KitchenSummary kitchen,
        ProfileSummary profile,
        Boolean active,
        LocalDateTime lastLogin,
        LocalDateTime createdAt
) {
    public record KitchenSummary(Integer id, String name) {
    }

    public record ProfileSummary(Integer id, String accessType) {
    }

    public static UserResponse fromEntity(User user) {
        var kitchen = user.getKitchen() != null
                ? new KitchenSummary(user.getKitchen().getId(), user.getKitchen().getName())
                : null;
        var profile = new ProfileSummary(user.getProfile().getId(), user.getProfile().getAccessType());

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                kitchen,
                profile,
                user.getActive(),
                user.getLastLogin(),
                user.getCreatedAt()
        );
    }
}

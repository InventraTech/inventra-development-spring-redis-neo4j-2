package com.inventra.api.infrastructure.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.inventra.api.core.domain.user.User;

// Regra de negócio: cada usuário (inclusive supervisor/admin) só acessa a cozinha
// do próprio cadastro (user.kitchen) — sem cozinha própria, sem acesso a nada com kitchenId.
@Component
public class KitchenAccessGuard {

    public void assertAccess(Integer kitchenId) {
        if (kitchenId == null) {
            return;
        }
        if (!hasAccess(kitchenId)) {
            throw new AccessDeniedException("Você não tem acesso a essa cozinha.");
        }
    }

    public boolean hasAccess(Integer kitchenId) {
        if (kitchenId == null) {
            return false;
        }
        User user = currentUser();
        return user.getKitchen() != null && user.getKitchen().getId().equals(kitchenId);
    }

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        return principal.getUser();
    }
}

package com.inventra.api.core.domain.profile;

// Tipos de acesso permitidos para auto-cadastro (/api/auth/register).
// ADMIN é criado só pelo bootstrap; supervisor tem regra extra de 1 por cozinha (ver UserService).
public enum AccessType {

    SUPERVISOR,
    ESTOQUISTA,
    COMPRADOR;

    public String toProfileCode() {
        return name().toLowerCase();
    }

    public static boolean isSupervisor(String accessTypeCode) {
        return SUPERVISOR.toProfileCode().equalsIgnoreCase(accessTypeCode);
    }
}

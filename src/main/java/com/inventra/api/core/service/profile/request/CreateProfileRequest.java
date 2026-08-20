package com.inventra.api.core.service.profile.request;

public record CreateProfileRequest(
    boolean tipo_acesso,
    String descricao
){
}

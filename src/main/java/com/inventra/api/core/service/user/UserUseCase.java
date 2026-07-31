package com.inventra.api.core.service.user;

import com.inventra.api.core.service.user.model.request.ChangePasswordRequest;
import com.inventra.api.core.service.user.model.request.CreateUserRequest;
import com.inventra.api.core.service.user.model.request.UpdateUserRequest;
import com.inventra.api.core.domain.user.User;

import java.util.UUID;

public interface UserUseCase {

    User create(CreateUserRequest request);

    User findById(UUID id);

    User findByEmail(String email);

    User update(UUID id, UpdateUserRequest request);

    void changePassword(UUID id, ChangePasswordRequest request);

    // já existe active, não precisa hard delete
    void activate(UUID id);

    void deactivate(UUID id);

    // atualiza lastLogin, chamado pelo fluxo de auth
    void registerLogin(UUID id);
}

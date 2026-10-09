package com.inventra.api.core.service.auth;

import com.inventra.api.core.service.auth.model.request.LoginRequest;
import com.inventra.api.core.service.auth.model.request.RefreshTokenRequest;
import com.inventra.api.core.service.auth.model.request.RegisterRequest;
import com.inventra.api.core.service.auth.model.response.LoginResponse;

public interface AuthUseCase {

    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}

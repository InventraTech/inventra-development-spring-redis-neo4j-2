package com.inventra.api.core.service.auth;

import com.inventra.api.core.service.auth.model.request.LoginRequest;
import com.inventra.api.core.service.auth.model.response.LoginResponse;

public interface AuthUseCase {

    LoginResponse login(LoginRequest request);
}

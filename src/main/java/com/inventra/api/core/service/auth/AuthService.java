package com.inventra.api.core.service.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.inventra.api.core.domain.user.User;
import com.inventra.api.core.service.auth.model.request.LoginRequest;
import com.inventra.api.core.service.auth.model.response.LoginResponse;
import com.inventra.api.core.service.user.UserUseCase;
import com.inventra.api.core.service.user.model.response.UserResponse;
import com.inventra.api.infrastructure.security.JwtService;
import com.inventra.api.infrastructure.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserUseCase userUseCase;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) result.getPrincipal();
        User user = principal.getUser();

        String token = jwtService.generateToken(principal);
        userUseCase.registerLogin(user.getId());

        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs() / 1000, UserResponse.fromEntity(user));
    }
}

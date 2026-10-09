package com.inventra.api.core.service.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.inventra.api.core.domain.profile.Profile;
import com.inventra.api.core.domain.user.User;
import com.inventra.api.core.service.auth.model.request.LoginRequest;
import com.inventra.api.core.service.auth.model.request.RefreshTokenRequest;
import com.inventra.api.core.service.auth.model.request.RegisterRequest;
import com.inventra.api.core.service.auth.model.response.LoginResponse;
import com.inventra.api.core.service.user.UserUseCase;
import com.inventra.api.core.service.user.model.response.UserResponse;
import com.inventra.api.infrastructure.exception.TooManyRequestsException;
import com.inventra.api.infrastructure.repository.ProfileRepository;
import com.inventra.api.infrastructure.security.JwtService;
import com.inventra.api.infrastructure.security.LoginAttemptService;
import com.inventra.api.infrastructure.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserUseCase userUseCase;
    private final ProfileRepository profileRepository;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest httpRequest;
    private final RefreshTokenService refreshTokenService;

    @Override
    public LoginResponse login(LoginRequest request) {
        String ip = httpRequest.getRemoteAddr();
        if (loginAttemptService.isBlocked(request.email(), ip)) {
            throw new TooManyRequestsException("Muitas tentativas de login. Aguarde alguns minutos e tente novamente.");
        }

        Authentication result;
        try {
            result = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            loginAttemptService.registerFailure(request.email(), ip);
            throw ex;
        }
        loginAttemptService.registerSuccess(request.email(), ip);

        UserPrincipal principal = (UserPrincipal) result.getPrincipal();
        User user = principal.getUser();

        String token = jwtService.generateToken(principal);
        userUseCase.registerLogin(user.getId());

        return buildResponse(user, token, issueRefreshToken(user));
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        // O enum AccessType define quais perfis são permitidos no auto-cadastro; a linha em tb_profile
        // (necessária porque tb_user.id_profile é NOT NULL) é criada no primeiro uso, se ainda não existir.
        String profileCode = request.accessType().toProfileCode();
        Profile profile = profileRepository.findByAccessType(profileCode)
                .orElseGet(() -> profileRepository.save(Profile.builder()
                        .accessType(profileCode)
                        .description("Criado automaticamente pelo auto-cadastro")
                        .build()));

        User user = userUseCase.registerSelf(request.name(), request.email(), request.password(), profile.getId());

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);

        return buildResponse(user, token, issueRefreshToken(user));
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.refreshToken());
        User user = rotation.user();
        String token = jwtService.generateToken(new UserPrincipal(user));
        return buildResponse(user, token, rotation.refreshToken());
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    // Se o Redis estiver fora do ar, o login ainda funciona: sem refresh token, o app renova no próximo login.
    private RefreshTokenService.IssuedRefreshToken issueRefreshToken(User user) {
        try {
            return refreshTokenService.issue(user);
        } catch (RuntimeException ex) {
            log.warn("Não foi possível emitir o refresh token do usuário {}: {}", user.getId(), ex.toString());
            return null;
        }
    }

    private LoginResponse buildResponse(User user, String token, RefreshTokenService.IssuedRefreshToken refresh) {
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs() / 1000,
                refresh == null ? null : refresh.token(),
                refresh == null ? null : refresh.expiresInSeconds(),
                UserResponse.fromEntity(user));
    }
}

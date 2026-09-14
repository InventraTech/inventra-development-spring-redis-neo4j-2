package com.inventra.api.infrastructure.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.inventra.api.core.domain.profile.Profile;
import com.inventra.api.core.domain.user.User;
import com.inventra.api.infrastructure.repository.ProfileRepository;
import com.inventra.api.infrastructure.repository.UserRepository;

// Resolve o ovo-e-galinha de POST /api/users agora exigir autenticação: cria o admin do bootstrap se ele
// ainda não existir. Checa por e-mail específico (não por tabela vazia) porque o banco de testes é
// compartilhado com outras disciplinas e já vem com dados — nunca está realmente vazio.
@Configuration
public class BootstrapAdminConfig {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminConfig.class);
    private static final String ADMIN_ACCESS_TYPE = "ADMIN";

    @Bean
    public ApplicationRunner seedInitialAdmin(UserRepository userRepository,
                                               ProfileRepository profileRepository,
                                               PasswordEncoder passwordEncoder,
                                               @Value("${app.bootstrap-admin.email:}") String email,
                                               @Value("${app.bootstrap-admin.password:}") String rawPassword) {
        return (ApplicationArguments args) -> {
            if (email.isBlank() || rawPassword.isBlank() || userRepository.existsByEmail(email)) {
                return;
            }

            Profile adminProfile = profileRepository.findByAccessType(ADMIN_ACCESS_TYPE)
                    .orElseGet(() -> profileRepository.save(Profile.builder()
                            .accessType(ADMIN_ACCESS_TYPE)
                            .description("Acesso administrativo completo")
                            .build()));

            User admin = User.builder()
                    .id(UUID.randomUUID())
                    .name("Administrador")
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .profile(adminProfile)
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.info("Usuário admin inicial criado: {}", email);
        };
    }
}

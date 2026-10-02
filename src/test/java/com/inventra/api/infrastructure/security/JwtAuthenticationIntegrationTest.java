package com.inventra.api.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventra.api.core.domain.profile.AccessType;
import com.inventra.api.core.domain.profile.Profile;
import com.inventra.api.core.domain.user.User;
import com.inventra.api.core.service.auth.model.request.LoginRequest;
import com.inventra.api.core.service.auth.model.request.RegisterRequest;
import com.inventra.api.core.service.auth.model.response.LoginResponse;
import com.inventra.api.infrastructure.client.openfoodfacts.OpenFoodFactsClient;
import com.inventra.api.infrastructure.repository.ProfileRepository;
import com.inventra.api.infrastructure.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationIntegrationTest {

    private static final String RAW_PASSWORD = "SenhaForte123";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Evita instanciar o RestClient real (HttpClient falha em ambientes sandboxed sem loopback).
    @MockitoBean
    private OpenFoodFactsClient openFoodFactsClient;

    private String email;

    @BeforeEach
    void seedUser() {
        // saveAndFlush: o login roda numa sessão JPA separada (por requisição via MockMvc),
        // que só enxerga o que já foi enviado ao banco na transação compartilhada do teste.
        Profile profile = profileRepository.findByAccessType("ADMIN")
                .orElseGet(() -> profileRepository.saveAndFlush(Profile.builder().accessType("ADMIN").build()));

        email = "test-" + UUID.randomUUID() + "@inventra.com";
        userRepository.saveAndFlush(User.builder()
                .id(UUID.randomUUID())
                .name("Usuário de Teste")
                .email(email)
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                .profile(profile)
                .active(true)
                .build());
    }

    @Test
    void validTokenGrantsAccessToProtectedEndpoint() throws Exception {
        String token = login(email, RAW_PASSWORD);

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = login(email, RAW_PASSWORD);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    // Auto-cadastro: precisa ser acessível sem token — quem se cadastra ainda não tem conta pra logar.
    @Test
    void registerIsPublicAndCreatesMissingProfile() throws Exception {
        // Sem semear o perfil: o enum AccessType basta, o perfil é criado no primeiro uso.
        assertThat(countProfiles("supervisor")).isZero();

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRegisterRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        LoginResponse response = objectMapper.readValue(body, LoginResponse.class);
        assertThat(response.token()).isNotBlank();
        assertThat(response.user().profile().accessType()).isEqualTo("supervisor");
        assertThat(countProfiles("supervisor")).isEqualTo(1);
    }

    @Test
    void registerReusesExistingProfileInsteadOfDuplicatingIt() throws Exception {
        ensureProfile("supervisor");

        Integer firstProfileId = registerAndGetProfileId();
        Integer secondProfileId = registerAndGetProfileId();

        assertThat(secondProfileId).isEqualTo(firstProfileId);
        assertThat(countProfiles("supervisor")).isEqualTo(1);
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String json = objectMapper.writeValueAsString(newRegisterRequest());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void tokenReturnedByRegisterGrantsAccessToTheNewUserOnlyWhenSent() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRegisterRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        LoginResponse registered = objectMapper.readValue(body, LoginResponse.class);
        String userUrl = "/api/users/" + registered.user().id();

        mockMvc.perform(get(userUrl).header(HttpHeaders.AUTHORIZATION, "Bearer " + registered.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(registered.user().email()));

        mockMvc.perform(get(userUrl))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerDoesNotAcceptAdminAccessType() throws Exception {
        String json = """
                {"name":"Maria Silva","email":"admin-try-%s@inventra.com","password":"%s","accessType":"ADMIN"}
                """.formatted(UUID.randomUUID(), RAW_PASSWORD);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    private Integer registerAndGetProfileId() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRegisterRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, LoginResponse.class).user().profile().id();
    }

    private long countProfiles(String accessType) {
        return profileRepository.findAll().stream()
                .filter(profile -> accessType.equals(profile.getAccessType()))
                .count();
    }

    private void ensureProfile(String accessType) {
        profileRepository.findByAccessType(accessType)
                .orElseGet(() -> profileRepository.saveAndFlush(Profile.builder().accessType(accessType).build()));
    }

    private RegisterRequest newRegisterRequest() {
        return new RegisterRequest("Maria Silva", "register-" + UUID.randomUUID() + "@inventra.com",
                RAW_PASSWORD, AccessType.SUPERVISOR);
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(body, LoginResponse.class).token();
    }
}

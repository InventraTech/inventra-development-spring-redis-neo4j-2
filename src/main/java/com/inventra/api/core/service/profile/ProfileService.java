package com.inventra.api.core.service.profile;

import java.util.List;

import org.springframework.stereotype.Service;

import com.inventra.api.core.domain.profile.Profile;
import com.inventra.api.core.service.profile.model.request.CreateProfileRequest;
import com.inventra.api.core.service.profile.model.request.UpdateProfileRequest;
import com.inventra.api.infrastructure.repository.ProfileRepository;
import com.inventra.api.infrastructure.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService implements ProfileUseCase {

    private final ProfileRepository repository;
    private final UserRepository userRepository;

    @Override
    public Profile create(CreateProfileRequest request) {
        if (repository.existsByAccessType(request.accessType())) {
            throw new RuntimeException("Já existe um perfil com esse tipo de acesso.");
        }

        Profile profile = Profile.builder()
            .accessType(request.accessType())
            .description(request.description())
            .build();

        return repository.save(profile);
    }

    @Override
    public Profile findById(Integer id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Perfil não encontrado."));
    }

    @Override
    public List<Profile> listAll() {
        return repository.findAll();
    }

    @Override
    public Profile update(Integer id, UpdateProfileRequest request) {
        Profile profile = findById(id);

        if (request.accessType() != null) {
            profile.setAccessType(request.accessType());
        }
        if (request.description() != null) {
            profile.setDescription(request.description());
        }

        return repository.save(profile);
    }

    @Override
    public void delete(Integer id) {
        Profile profile = findById(id);

        if (userRepository.existsByProfileId(id)) {
            throw new RuntimeException("Não é possível excluir: existem usuários vinculados a esse perfil.");
        }

        repository.delete(profile);
    }
}

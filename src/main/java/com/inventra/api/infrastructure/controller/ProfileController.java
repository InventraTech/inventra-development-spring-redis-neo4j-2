package com.inventra.api.infrastructure.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inventra.api.core.domain.profile.Profile;
import com.inventra.api.core.service.profile.ProfileUseCase;
import com.inventra.api.core.service.profile.model.request.CreateProfileRequest;
import com.inventra.api.core.service.profile.model.request.UpdateProfileRequest;
import com.inventra.api.core.service.profile.model.response.ProfileResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileUseCase useCase;

    @PostMapping
    public ResponseEntity<ProfileResponse> create(@Valid @RequestBody CreateProfileRequest request) {
        Profile created = useCase.create(request);
        ProfileResponse response = ProfileResponse.fromEntity(created);
        return ResponseEntity.created(URI.create("/api/profiles/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(ProfileResponse.fromEntity(useCase.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<ProfileResponse>> listAll() {
        List<ProfileResponse> responses = useCase.listAll().stream()
                .map(ProfileResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> update(@PathVariable Integer id,
                                                   @Valid @RequestBody UpdateProfileRequest request) {
        Profile updated = useCase.update(id, request);
        return ResponseEntity.ok(ProfileResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        useCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}

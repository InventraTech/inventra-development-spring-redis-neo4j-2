package com.inventra.api.infrastructure.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.service.kitchen.KitchenUseCase;
import com.inventra.api.core.service.kitchen.model.request.CreateKitchenRequest;
import com.inventra.api.core.service.kitchen.model.request.UpdateKitchenRequest;
import com.inventra.api.core.service.kitchen.model.response.KitchenLookupResponse;
import com.inventra.api.core.service.kitchen.model.response.KitchenResponse;
import com.inventra.api.infrastructure.security.Roles;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/kitchens")
@RequiredArgsConstructor
public class KitchenController {

    private final KitchenUseCase useCase;

    @PreAuthorize(Roles.SUPERVISOR)
    @PostMapping
    public ResponseEntity<KitchenResponse> create(@Valid @RequestBody CreateKitchenRequest request) {
        Kitchen created = useCase.create(request);
        KitchenResponse response = KitchenResponse.fromEntity(created);
        return ResponseEntity.created(URI.create("/api/kitchens/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KitchenResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(KitchenResponse.fromEntity(useCase.findById(id)));
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<KitchenLookupResponse> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(KitchenLookupResponse.fromEntity(useCase.findByCode(code)));
    }

    @GetMapping
    public ResponseEntity<List<KitchenResponse>> listActive() {
        List<KitchenResponse> responses = useCase.listActive().stream()
                .map(KitchenResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize(Roles.SUPERVISOR)
    @PutMapping("/{id}")
    public ResponseEntity<KitchenResponse> update(@PathVariable Integer id,
                                                   @Valid @RequestBody UpdateKitchenRequest request) {
        Kitchen updated = useCase.update(id, request);
        return ResponseEntity.ok(KitchenResponse.fromEntity(updated));
    }

    @PreAuthorize(Roles.SUPERVISOR)
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Integer id) {
        useCase.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(Roles.SUPERVISOR)
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        useCase.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

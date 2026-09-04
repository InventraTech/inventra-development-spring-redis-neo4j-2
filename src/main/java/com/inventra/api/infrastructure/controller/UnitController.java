package com.inventra.api.infrastructure.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inventra.api.core.domain.unit.Unit;
import com.inventra.api.core.service.unit.UnitUseCase;
import com.inventra.api.core.service.unit.model.request.CreateUnitRequest;
import com.inventra.api.core.service.unit.model.request.UpdateUnitRequest;
import com.inventra.api.core.service.unit.model.response.UnitResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitUseCase useCase;

    @PostMapping
    public ResponseEntity<UnitResponse> create(@Valid @RequestBody CreateUnitRequest request) {
        Unit created = useCase.create(request);
        UnitResponse response = UnitResponse.fromEntity(created);
        return ResponseEntity.created(URI.create("/api/units/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(UnitResponse.fromEntity(useCase.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<UnitResponse>> listAll() {
        List<UnitResponse> responses = useCase.listAll().stream()
                .map(UnitResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnitResponse> update(@PathVariable Integer id,
                                                @Valid @RequestBody UpdateUnitRequest request) {
        Unit updated = useCase.update(id, request);
        return ResponseEntity.ok(UnitResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        useCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}

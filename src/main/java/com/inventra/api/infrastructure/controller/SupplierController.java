package com.inventra.api.infrastructure.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inventra.api.core.domain.supplier.Supplier;
import com.inventra.api.core.service.supplier.SupplierUseCase;
import com.inventra.api.core.service.supplier.model.request.CreateSupplierRequest;
import com.inventra.api.core.service.supplier.model.request.UpdateSupplierRequest;
import com.inventra.api.core.service.supplier.model.response.SupplierResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierUseCase useCase;

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        Supplier created = useCase.create(request);
        SupplierResponse response = SupplierResponse.fromEntity(created);
        return ResponseEntity.created(URI.create("/api/suppliers/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(SupplierResponse.fromEntity(useCase.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponse>> listActive() {
        List<SupplierResponse> responses = useCase.listActive().stream()
                .map(SupplierResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(@PathVariable Integer id,
                                                    @Valid @RequestBody UpdateSupplierRequest request) {
        Supplier updated = useCase.update(id, request);
        return ResponseEntity.ok(SupplierResponse.fromEntity(updated));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Integer id) {
        useCase.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        useCase.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

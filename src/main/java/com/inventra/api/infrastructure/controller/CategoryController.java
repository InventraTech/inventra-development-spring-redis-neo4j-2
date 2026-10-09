package com.inventra.api.infrastructure.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.inventra.api.core.domain.category.Category;
import com.inventra.api.core.service.category.CategoryUseCase;
import com.inventra.api.core.service.category.model.request.CreateCategoryRequest;
import com.inventra.api.core.service.category.model.request.UpdateCategoryRequest;
import com.inventra.api.core.service.category.model.response.CategoryResponse;
import com.inventra.api.infrastructure.security.Roles;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryUseCase useCase;

    @PreAuthorize(Roles.SUPERVISOR)
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        Category created = useCase.create(request);
        CategoryResponse response = CategoryResponse.fromEntity(created);
        return ResponseEntity.created(URI.create("/api/categories/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(CategoryResponse.fromEntity(useCase.findById(id)));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listAll() {
        List<CategoryResponse> responses = useCase.listAll().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize(Roles.SUPERVISOR)
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Integer id,
                                                    @Valid @RequestBody UpdateCategoryRequest request) {
        Category updated = useCase.update(id, request);
        return ResponseEntity.ok(CategoryResponse.fromEntity(updated));
    }

    @PreAuthorize(Roles.SUPERVISOR)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        useCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}

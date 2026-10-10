package com.inventra.api.infrastructure.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.inventra.api.core.domain.alert.Alert;
import com.inventra.api.core.service.alert.AlertUseCase;
import com.inventra.api.core.service.alert.model.request.CreateAlertRequest;
import com.inventra.api.core.service.alert.model.response.AlertResponse;
import com.inventra.api.infrastructure.security.Roles;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@PreAuthorize(Roles.STOCK)
@RestController
@Tag(name = "Alertas", description = "Alertas de estoque e validade por cozinha")
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertUseCase useCase;

    @Operation(summary = "Cria um alerta manualmente")
    @PreAuthorize(Roles.SUPERVISOR)
    @PostMapping
    public ResponseEntity<AlertResponse> create(@Valid @RequestBody CreateAlertRequest request) {
        Alert created = useCase.create(request);
        AlertResponse response = AlertResponse.fromEntity(created);
        return ResponseEntity.created(URI.create("/api/alerts/" + response.id())).body(response);
    }

    @Operation(summary = "Busca um alerta pelo id")
    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(AlertResponse.fromEntity(useCase.findById(id)));
    }

    @Operation(summary = "Lista os alertas de uma cozinha (opcionalmente só os não lidos)")
    @GetMapping
    public ResponseEntity<List<AlertResponse>> listByKitchen(
            @RequestParam Integer kitchenId,
            @RequestParam(required = false, defaultValue = "false") boolean unread) {
        List<Alert> alerts = unread ? useCase.listUnreadByKitchen(kitchenId) : useCase.listByKitchen(kitchenId);
        List<AlertResponse> responses = alerts.stream()
                .map(AlertResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Marca um alerta como lido")
    @PatchMapping("/{id}/read")
    public ResponseEntity<AlertResponse> markAsRead(@PathVariable Integer id) {
        return ResponseEntity.ok(AlertResponse.fromEntity(useCase.markAsRead(id)));
    }

    @Operation(summary = "Exclui um alerta")
    @PreAuthorize(Roles.SUPERVISOR)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        useCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}

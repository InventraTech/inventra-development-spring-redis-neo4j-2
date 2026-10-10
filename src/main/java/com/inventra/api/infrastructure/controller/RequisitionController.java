package com.inventra.api.infrastructure.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.inventra.api.core.domain.requisition.Requisition;
import com.inventra.api.core.domain.requisition.enums.RequisitionStatus;
import com.inventra.api.core.service.requisition.RequisitionUseCase;
import com.inventra.api.core.service.requisition.model.request.AddRequisitionItemRequest;
import com.inventra.api.core.service.requisition.model.request.CancelRequisitionRequest;
import com.inventra.api.core.service.requisition.model.request.CreateRequisitionRequest;
import com.inventra.api.core.service.requisition.model.request.RejectRequisitionRequest;
import com.inventra.api.core.service.requisition.model.response.RequisitionItemResponse;
import com.inventra.api.core.service.requisition.model.response.RequisitionResponse;
import com.inventra.api.infrastructure.security.Roles;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@PreAuthorize(Roles.REQUISITION)
@RestController
@Tag(name = "Requisições", description = "Requisições de consumo, transferência e compra")
@RequestMapping("/api/requisitions")
@RequiredArgsConstructor
public class RequisitionController {

    private final RequisitionUseCase useCase;

    @Operation(summary = "Cria uma requisição")
    @PostMapping
    public ResponseEntity<RequisitionResponse> create(@Valid @RequestBody CreateRequisitionRequest request) {
        Requisition created = useCase.create(request);
        RequisitionResponse response = RequisitionResponse.fromEntity(created);
        return ResponseEntity.created(URI.create("/api/requisitions/" + response.id())).body(response);
    }

    @Operation(summary = "Lista requisições com filtros opcionais de cozinha, status e solicitante")
    @GetMapping
    public ResponseEntity<List<RequisitionResponse>> list(@RequestParam(required = false) Integer kitchenId,
                                                            @RequestParam(required = false) RequisitionStatus status,
                                                            @RequestParam(required = false) UUID requesterId) {
        List<Requisition> requisitions;
        if (kitchenId != null) {
            requisitions = useCase.listByKitchen(kitchenId);
        } else if (status != null) {
            requisitions = useCase.listByStatus(status);
        } else if (requesterId != null) {
            requisitions = useCase.listByRequester(requesterId);
        } else {
            throw new IllegalArgumentException("Informe kitchenId, status ou requesterId.");
        }

        List<RequisitionResponse> responses = requisitions.stream()
                .map(RequisitionResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Adiciona um item à requisição")
    @PostMapping("/{id}/items")
    public ResponseEntity<RequisitionResponse> addItem(@PathVariable Integer id,
                                                        @Valid @RequestBody AddRequisitionItemRequest request) {
        Requisition updated = useCase.addItem(id, request);
        return ResponseEntity.ok(RequisitionResponse.fromEntity(updated));
    }

    @Operation(summary = "Remove um item da requisição")
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<RequisitionResponse> removeItem(@PathVariable Integer id, @PathVariable Integer itemId) {
        Requisition updated = useCase.removeItem(id, itemId);
        return ResponseEntity.ok(RequisitionResponse.fromEntity(updated));
    }

    @Operation(summary = "Lista os itens da requisição")
    @GetMapping("/{id}/items")
    public ResponseEntity<List<RequisitionItemResponse>> listItems(@PathVariable Integer id) {
        List<RequisitionItemResponse> responses = useCase.listItems(id).stream()
                .map(RequisitionItemResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Envia a requisição para análise")
    @PatchMapping("/{id}/submit")
    public ResponseEntity<RequisitionResponse> submit(@PathVariable Integer id) {
        return ResponseEntity.ok(RequisitionResponse.fromEntity(useCase.submit(id)));
    }

    @Operation(summary = "Aprova a requisição (procedure sp_approve_requisition)")
    @PreAuthorize(Roles.SUPERVISOR)
    @PatchMapping("/{id}/approve")
    public ResponseEntity<RequisitionResponse> approve(@PathVariable Integer id) {
        Requisition approved = useCase.approve(id);
        return ResponseEntity.ok(RequisitionResponse.fromEntity(approved));
    }

    @Operation(summary = "Rejeita a requisição com motivo (procedure sp_reject_requisition)")
    @PreAuthorize(Roles.SUPERVISOR)
    @PatchMapping("/{id}/reject")
    public ResponseEntity<RequisitionResponse> reject(@PathVariable Integer id,
                                                       @Valid @RequestBody RejectRequisitionRequest request) {
        Requisition rejected = useCase.reject(id, request.reason());
        return ResponseEntity.ok(RequisitionResponse.fromEntity(rejected));
    }

    @Operation(summary = "Cancela a requisição com motivo (procedure sp_cancel_requisition)")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RequisitionResponse> cancel(@PathVariable Integer id,
                                                       @Valid @RequestBody CancelRequisitionRequest request) {
        Requisition cancelled = useCase.cancel(id, request.reason());
        return ResponseEntity.ok(RequisitionResponse.fromEntity(cancelled));
    }
}

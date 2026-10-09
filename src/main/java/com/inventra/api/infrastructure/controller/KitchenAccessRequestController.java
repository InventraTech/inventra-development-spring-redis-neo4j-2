package com.inventra.api.infrastructure.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inventra.api.core.service.kitchenaccess.KitchenAccessRequest;
import com.inventra.api.core.service.kitchenaccess.KitchenAccessRequestService;
import com.inventra.api.core.service.kitchenaccess.KitchenAccessRequestStatus;
import com.inventra.api.core.service.kitchenaccess.model.request.CreateKitchenAccessRequest;
import com.inventra.api.core.service.kitchenaccess.model.request.RejectKitchenAccessRequest;
import com.inventra.api.core.service.kitchenaccess.model.response.KitchenAccessRequestResponse;
import com.inventra.api.infrastructure.security.Roles;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/kitchen-access-requests")
@RequiredArgsConstructor
public class KitchenAccessRequestController {

    private final KitchenAccessRequestService service;

    // qualquer usuário autenticado sem cozinha
    @PostMapping
    public ResponseEntity<KitchenAccessRequestResponse> request(@Valid @RequestBody CreateKitchenAccessRequest body) {
        KitchenAccessRequestResponse response = toResponse(service.request(body.code()));
        return ResponseEntity.created(URI.create("/api/kitchen-access-requests/" + response.id())).body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<KitchenAccessRequestResponse> mine() {
        return ResponseEntity.ok(toResponse(service.findMine()));
    }

    @PreAuthorize(Roles.SUPERVISOR)
    @GetMapping
    public ResponseEntity<List<KitchenAccessRequestResponse>> list(
            @RequestParam(required = false) KitchenAccessRequestStatus status) {
        return ResponseEntity.ok(service.listForMyKitchen(status).stream().map(this::toResponse).toList());
    }

    @PreAuthorize(Roles.SUPERVISOR)
    @PostMapping("/{id}/approve")
    public ResponseEntity<KitchenAccessRequestResponse> approve(@PathVariable String id) {
        return ResponseEntity.ok(toResponse(service.approve(id)));
    }

    @PreAuthorize(Roles.SUPERVISOR)
    @PostMapping("/{id}/reject")
    public ResponseEntity<KitchenAccessRequestResponse> reject(@PathVariable String id,
                                                                @Valid @RequestBody(required = false) RejectKitchenAccessRequest body) {
        return ResponseEntity.ok(toResponse(service.reject(id, body == null ? null : body.reason())));
    }

    private KitchenAccessRequestResponse toResponse(KitchenAccessRequest request) {
        return KitchenAccessRequestResponse.from(request, service.pendingTtl());
    }
}

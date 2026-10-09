package com.inventra.api.core.service.kitchenaccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.user.User;
import com.inventra.api.core.service.kitchen.KitchenUseCase;
import com.inventra.api.infrastructure.exception.BusinessRuleException;
import com.inventra.api.infrastructure.exception.ResourceNotFoundException;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.UserRepository;
import com.inventra.api.infrastructure.security.KitchenAccessGuard;

// Fluxo: usuário sem cozinha pede entrada pelo código -> supervisor da cozinha aprova ou recusa. Aprovar grava
// a cozinha no usuário; como o vínculo é lido do banco a cada requisição, o token dele continua valendo.
@Service
public class KitchenAccessRequestService implements KitchenAccessRequestUseCase {

    private final KitchenAccessRequestStore store;
    private final KitchenUseCase kitchenUseCase;
    private final KitchenRepository kitchenRepository;
    private final UserRepository userRepository;
    private final KitchenAccessGuard accessGuard;
    private final Duration pendingTtl;

    public KitchenAccessRequestService(KitchenAccessRequestStore store, KitchenUseCase kitchenUseCase,
                                       KitchenRepository kitchenRepository, UserRepository userRepository,
                                       KitchenAccessGuard accessGuard,
                                       @Value("${app.kitchen-access-request.ttl:7d}") Duration pendingTtl) {
        this.store = store;
        this.kitchenUseCase = kitchenUseCase;
        this.kitchenRepository = kitchenRepository;
        this.userRepository = userRepository;
        this.accessGuard = accessGuard;
        this.pendingTtl = pendingTtl;
    }

    public Duration pendingTtl() {
        return pendingTtl;
    }

    @Override
    public KitchenAccessRequest request(String code) {
        User current = accessGuard.currentUser();
        if (accessGuard.currentKitchenId() != null) {
            throw new BusinessRuleException("Você já está vinculado a uma cozinha.");
        }
        // a busca por código já limita as tentativas do usuário e ignora cozinha desativada
        Kitchen kitchen = kitchenUseCase.findByCode(code);

        if (store.findLatestByUser(current.getId()).filter(KitchenAccessRequest::isPending).isPresent()) {
            throw new BusinessRuleException("Você já tem uma solicitação de entrada pendente.");
        }

        KitchenAccessRequest request = new KitchenAccessRequest(
                UUID.randomUUID().toString(),
                current.getId(),
                current.getName(),
                current.getEmail(),
                kitchen.getId(),
                kitchen.getName(),
                KitchenAccessRequestStatus.PENDING,
                Instant.now(),
                null,
                null,
                null);
        store.savePending(request, pendingTtl);
        return request;
    }

    @Override
    public KitchenAccessRequest findMine() {
        return store.findLatestByUser(accessGuard.currentUser().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Você não tem solicitação de entrada em cozinha."));
    }

    @Override
    public List<KitchenAccessRequest> listForMyKitchen(KitchenAccessRequestStatus status) {
        assertSupervisor();
        Integer kitchenId = accessGuard.currentKitchenId();
        if (kitchenId == null) {
            return List.of();
        }
        return store.listByKitchen(kitchenId).stream()
            .filter(request -> status == null || request.status() == status)
            .toList();
    }

    @Override
    @Transactional
    public KitchenAccessRequest approve(String id) {
        KitchenAccessRequest request = loadPendingForMyKitchen(id);

        User applicant = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (!Boolean.TRUE.equals(applicant.getActive())) {
            throw new BusinessRuleException("O usuário está desativado.");
        }
        if (applicant.getKitchen() != null) {
            throw new BusinessRuleException("O usuário já está vinculado a uma cozinha.");
        }
        Kitchen kitchen = kitchenRepository.findById(request.kitchenId())
            .orElseThrow(() -> new ResourceNotFoundException("Cozinha não encontrada."));
        if (!Boolean.TRUE.equals(kitchen.getActive())) {
            throw new BusinessRuleException("Cozinha desativada: reative a cozinha para aprovar entradas.");
        }

        // Postgres primeiro: se a decisão no Redis falhar (ou perder a corrida para outro supervisor),
        // a exceção desfaz o vínculo.
        applicant.setKitchen(kitchen);
        userRepository.saveAndFlush(applicant);
        decide(request, KitchenAccessRequestStatus.APPROVED, null);
        return store.find(id).orElse(request);
    }

    @Override
    public KitchenAccessRequest reject(String id, String reason) {
        KitchenAccessRequest request = loadPendingForMyKitchen(id);
        decide(request, KitchenAccessRequestStatus.REJECTED, reason);
        return store.find(id).orElse(request);
    }

    private void decide(KitchenAccessRequest request, KitchenAccessRequestStatus status, String reason) {
        boolean decided = store.decide(request.id(), status, accessGuard.currentUser().getId(), reason, Instant.now());
        if (!decided) {
            throw new BusinessRuleException("Essa solicitação já foi decidida ou expirou.");
        }
    }

    private KitchenAccessRequest loadPendingForMyKitchen(String id) {
        assertSupervisor();
        KitchenAccessRequest request = store.find(id)
            .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada ou expirada."));
        accessGuard.assertAccess(request.kitchenId());
        if (!request.isPending()) {
            throw new BusinessRuleException("Essa solicitação já foi decidida.");
        }
        return request;
    }

    private void assertSupervisor() {
        if (!accessGuard.isSupervisor()) {
            throw new org.springframework.security.access.AccessDeniedException("Somente supervisor pode gerenciar solicitações de entrada.");
        }
    }
}

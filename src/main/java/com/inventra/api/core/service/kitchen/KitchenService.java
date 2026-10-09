package com.inventra.api.core.service.kitchen;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventra.api.core.service.kitchen.model.request.CreateKitchenRequest;
import com.inventra.api.core.service.kitchen.model.request.UpdateKitchenRequest;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.user.User;
import com.inventra.api.infrastructure.exception.BusinessRuleException;
import com.inventra.api.infrastructure.exception.ResourceNotFoundException;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.UserRepository;
import com.inventra.api.infrastructure.security.KitchenAccessGuard;
import com.inventra.api.infrastructure.security.KitchenCodeLookupLimiter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KitchenService implements KitchenUseCase {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final KitchenRepository repository;
    private final UserRepository userRepository;
    private final KitchenAccessGuard accessGuard;
    private final KitchenCodeGenerator codeGenerator;
    private final KitchenCodeLookupLimiter lookupLimiter;

    @Override
    @Transactional
    public Kitchen create(CreateKitchenRequest request) {
        // Cada usuário tem uma cozinha só: quem cria a cozinha é vinculado a ela. Sem isso, a cozinha
        // nasceria inacessível, já que ninguém pode vincular usuário a uma cozinha que não é a sua.
        if (accessGuard.currentKitchenId() != null) {
            throw new BusinessRuleException("Você já está vinculado a uma cozinha.");
        }

        Kitchen kitchen = Kitchen.builder()
            .name(request.name())
            .code(generateUniqueCode())
            .address(request.address())
            .active(true)
            .build();
        Kitchen saved = repository.save(kitchen);

        User creator = userRepository.findById(accessGuard.currentUser().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        creator.setKitchen(saved);
        userRepository.save(creator);

        return saved;
    }

    // O código é a chave que liga usuários à cozinha: é gerado aqui, nunca vem do cliente.
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (!repository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new BusinessRuleException("Não foi possível gerar o código da cozinha. Tente novamente.");
    }

    @Override
    public Kitchen findById(Integer id) {
        Kitchen kitchen = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cozinha não encontrada."));
        accessGuard.assertAccess(kitchen.getId());
        return kitchen;
    }

    @Override
    public Kitchen findByCode(String code) {
        // aberta a qualquer usuário autenticado (quem ainda não tem cozinha precisa dela para pedir entrada);
        // por isso limita consultas por usuário e não revela cozinha desativada
        lookupLimiter.register(accessGuard.currentUser().getId());
        return repository.findByCodeIgnoreCase(code.trim())
            .filter(kitchen -> Boolean.TRUE.equals(kitchen.getActive()))
            .orElseThrow(() -> new ResourceNotFoundException("Cozinha não encontrada."));
    }

    @Override
    public List<Kitchen> listActive() {
        // cada usuário só enxerga a própria cozinha: busca só ela, em vez de carregar todas e filtrar
        Integer kitchenId = accessGuard.currentKitchenId();
        if (kitchenId == null) {
            return List.of();
        }
        return repository.findById(kitchenId)
            .filter(kitchen -> Boolean.TRUE.equals(kitchen.getActive()))
            .map(List::of)
            .orElse(List.of());
    }

    @Override
    public Kitchen update(Integer id, UpdateKitchenRequest request) {
        accessGuard.assertAccess(id);
        Kitchen kitchen = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cozinha não encontrada."));

        if (request.name() != null) {
            kitchen.setName(request.name());
        }
        if (request.address() != null) {
            kitchen.setAddress(request.address());
        }

        return repository.save(kitchen);
    }

    @Override
    public void activate(Integer id) {
        accessGuard.assertAccess(id);
        Kitchen kitchen = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cozinha não encontrada."));
        kitchen.setActive(true);
        repository.save(kitchen);
    }

    @Override
    public void deactivate(Integer id) {
        accessGuard.assertAccess(id);
        Kitchen kitchen = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cozinha não encontrada."));
        kitchen.setActive(false);
        repository.save(kitchen);
    }
}

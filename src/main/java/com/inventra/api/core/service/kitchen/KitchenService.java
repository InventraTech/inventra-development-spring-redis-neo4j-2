package com.inventra.api.core.service.kitchen;

import java.util.List;

import org.springframework.stereotype.Service;

import com.inventra.api.core.service.kitchen.model.request.CreateKitchenRequest;
import com.inventra.api.core.service.kitchen.model.request.UpdateKitchenRequest;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.infrastructure.exception.BusinessRuleException;
import com.inventra.api.infrastructure.exception.ResourceNotFoundException;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.security.KitchenAccessGuard;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KitchenService implements KitchenUseCase {

    private final KitchenRepository repository;
    private final KitchenAccessGuard accessGuard;

    @Override
    public Kitchen create(CreateKitchenRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new BusinessRuleException("Já existe uma cozinha com esse código.");
        }

        Kitchen kitchen = Kitchen.builder()
            .name(request.name())
            .code(request.code())
            .address(request.address())
            .active(true)
            .build();

        return repository.save(kitchen);
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
        Kitchen kitchen = repository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Cozinha não encontrada."));
        accessGuard.assertAccess(kitchen.getId());
        return kitchen;
    }

    @Override
    public List<Kitchen> listActive() {
        return repository.findByActiveTrue().stream()
            .filter(kitchen -> accessGuard.hasAccess(kitchen.getId()))
            .toList();
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

package com.inventra.api.core.service.kitchen;

import java.util.List;

import org.springframework.stereotype.Service;

import com.inventra.api.core.service.kitchen.model.request.CreateKitchenRequest;
import com.inventra.api.core.service.kitchen.model.request.UpdateKitchenRequest;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.infrastructure.repository.KitchenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KitchenService implements KitchenUseCase {

    private final KitchenRepository repository;

    @Override
    public Kitchen create(CreateKitchenRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new RuntimeException("Já existe uma cozinha com esse código.");
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
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cozinha não encontrada."));
    }

    @Override
    public Kitchen findByCode(String code) {
        return repository.findByCode(code)
            .orElseThrow(() -> new RuntimeException("Cozinha não encontrada."));
    }

    @Override
    public List<Kitchen> listActive() {
        return repository.findByActiveTrue();
    }

    @Override
    public Kitchen update(Integer id, UpdateKitchenRequest request) {
        Kitchen kitchen = findById(id);

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
        Kitchen kitchen = findById(id);
        kitchen.setActive(true);
        repository.save(kitchen);
    }

    @Override
    public void deactivate(Integer id) {
        Kitchen kitchen = findById(id);
        kitchen.setActive(false);
        repository.save(kitchen);
    }
}

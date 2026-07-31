package com.inventra.api.core.service.kitchen;

import java.util.List;

import com.inventra.api.core.service.kitchen.model.request.CreateKitchenRequest;
import com.inventra.api.core.service.kitchen.model.request.UpdateKitchenRequest;
import com.inventra.api.core.domain.kitchen.Kitchen;

public interface KitchenUseCase {

    Kitchen create(CreateKitchenRequest request);

    Kitchen findById(Integer id);

    Kitchen findByCode(String code);

    List<Kitchen> listActive();

    Kitchen update(Integer id, UpdateKitchenRequest request);

    void activate(Integer id);

    void deactivate(Integer id);
}

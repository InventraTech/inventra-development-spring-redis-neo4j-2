package com.inventra.api.application.service.Kitchen;

import java.util.List;

import com.inventra.api.application.service.Kitchen.model.request.CreateKitchenRequest;
import com.inventra.api.application.service.Kitchen.model.request.UpdateKitchenRequest;
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

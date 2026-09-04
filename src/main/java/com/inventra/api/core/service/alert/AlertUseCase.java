package com.inventra.api.core.service.alert;

import java.util.List;

import com.inventra.api.core.domain.alert.Alert;
import com.inventra.api.core.service.alert.model.request.CreateAlertRequest;

public interface AlertUseCase {

    Alert create(CreateAlertRequest request);

    Alert findById(Integer id);

    List<Alert> listByKitchen(Integer kitchenId);

    List<Alert> listUnreadByKitchen(Integer kitchenId);

    Alert markAsRead(Integer id);

    void delete(Integer id);
}

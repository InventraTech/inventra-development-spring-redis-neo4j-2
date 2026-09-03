package com.inventra.api.core.service.alert;

import java.util.List;

import org.springframework.stereotype.Service;

import com.inventra.api.core.domain.alert.Alert;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.product.Product;
import com.inventra.api.core.domain.stock.StockBatch;
import com.inventra.api.core.service.alert.model.request.CreateAlertRequest;
import com.inventra.api.infrastructure.repository.AlertRepository;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.ProductRepository;
import com.inventra.api.infrastructure.repository.StockBatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertService implements AlertUseCase {

    private final AlertRepository repository;
    private final KitchenRepository kitchenRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;

    @Override
    public Alert create(CreateAlertRequest request) {
        Kitchen kitchen = kitchenRepository.findById(request.kitchenId())
            .orElseThrow(() -> new RuntimeException("Cozinha não encontrada."));

        Product product = null;
        if (request.productId() != null) {
            product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado."));
        }

        StockBatch batch = null;
        if (request.batchId() != null) {
            batch = stockBatchRepository.findById(request.batchId())
                .orElseThrow(() -> new RuntimeException("Lote não encontrado."));
        }

        Alert alert = Alert.builder()
            .type(request.type())
            .severity(request.severity())
            .batch(batch)
            .product(product)
            .kitchen(kitchen)
            .message(request.message())
            .read(false)
            .build();

        return repository.save(alert);
    }

    @Override
    public Alert findById(Integer id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta não encontrado."));
    }

    @Override
    public List<Alert> listByKitchen(Integer kitchenId) {
        return repository.findByKitchenId(kitchenId);
    }

    @Override
    public List<Alert> listUnreadByKitchen(Integer kitchenId) {
        return repository.findByKitchenIdAndReadFalse(kitchenId);
    }

    @Override
    public Alert markAsRead(Integer id) {
        Alert alert = findById(id);
        alert.setRead(true);
        return repository.save(alert);
    }

    @Override
    public void delete(Integer id) {
        repository.delete(findById(id));
    }
}

package com.inventra.api.application.service.StockBatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.inventra.api.application.service.StockBatch.model.request.RegisterStockEntryRequest;
import com.inventra.api.application.service.StockBatch.model.response.LowStockAlertResponse;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.product.Product;
import com.inventra.api.core.domain.product.ProductKitchenParameter;
import com.inventra.api.core.domain.stock.StockBatch;
import com.inventra.api.core.domain.stock.enums.StockBatchStatus;
import com.inventra.api.core.domain.supplier.Supplier;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.ProductKitchenParameterRepository;
import com.inventra.api.infrastructure.repository.ProductRepository;
import com.inventra.api.infrastructure.repository.StockBatchRepository;
import com.inventra.api.infrastructure.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockBatchService implements StockBatchUseCase {

    private final StockBatchRepository repository;
    private final ProductRepository productRepository;
    private final KitchenRepository kitchenRepository;
    private final SupplierRepository supplierRepository;
    private final ProductKitchenParameterRepository productKitchenParameterRepository;

    @Override
    public StockBatch registerEntry(RegisterStockEntryRequest request) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new RuntimeException("Produto não encontrado."));
        Kitchen kitchen = kitchenRepository.findById(request.kitchenId())
            .orElseThrow(() -> new RuntimeException("Cozinha não encontrada."));

        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new RuntimeException("Fornecedor não encontrado."));
        }

        StockBatch batch = StockBatch.builder()
            .product(product)
            .kitchen(kitchen)
            .supplier(supplier)
            .batchNumber(request.batchNumber())
            .invoiceNumber(request.invoiceNumber())
            .initialQuantity(request.initialQuantity())
            .currentQuantity(request.initialQuantity())
            .entryDate(request.entryDate())
            .expirationDate(request.expirationDate())
            .unitPrice(request.unitPrice())
            .status(StockBatchStatus.ATIVO)
            .build();

        return repository.save(batch);
    }

    @Override
    public StockBatch consume(Integer batchId, BigDecimal quantity) {
        StockBatch batch = repository.findById(batchId)
            .orElseThrow(() -> new RuntimeException("Lote não encontrado."));

        if (quantity.compareTo(batch.getCurrentQuantity()) > 0) {
            throw new RuntimeException("Quantidade solicitada maior que o saldo do lote.");
        }

        batch.setCurrentQuantity(batch.getCurrentQuantity().subtract(quantity));
        if (batch.getCurrentQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            batch.setStatus(StockBatchStatus.BAIXA);
        }

        return repository.save(batch);
    }

    @Override
    public void consumeForProduct(Integer kitchenId, Long productId, BigDecimal quantity) {
        List<StockBatch> batches = repository
            .findByKitchenIdAndProductIdAndStatusOrderByExpirationDateAscEntryDateAsc(
                kitchenId, productId, StockBatchStatus.ATIVO);

        BigDecimal remaining = quantity;
        for (StockBatch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal taken = batch.getCurrentQuantity().min(remaining);
            batch.setCurrentQuantity(batch.getCurrentQuantity().subtract(taken));
            if (batch.getCurrentQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                batch.setStatus(StockBatchStatus.BAIXA);
            }
            repository.save(batch);

            remaining = remaining.subtract(taken);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Estoque insuficiente para atender a quantidade solicitada.");
        }
    }

    @Override
    public StockBatch adjust(Integer batchId, BigDecimal newQuantity) {
        StockBatch batch = repository.findById(batchId)
            .orElseThrow(() -> new RuntimeException("Lote não encontrado."));

        batch.setCurrentQuantity(newQuantity);
        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            batch.setStatus(StockBatchStatus.BAIXA);
        } else if (batch.getStatus() == StockBatchStatus.BAIXA) {
            batch.setStatus(StockBatchStatus.ATIVO);
        }

        return repository.save(batch);
    }

    @Override
    public List<StockBatch> findExpiringSoon(Integer kitchenId, int days) {
        LocalDate today = LocalDate.now();
        return repository.findByKitchenIdAndStatusAndExpirationDateBetween(
            kitchenId, StockBatchStatus.ATIVO, today, today.plusDays(days));
    }

    @Override
    public List<LowStockAlertResponse> findLowStock() {
        List<LowStockAlertResponse> alerts = new ArrayList<>();

        for (ProductKitchenParameter parameter : productKitchenParameterRepository.findAll()) {
            BigDecimal current = repository.sumActiveQuantity(
                parameter.getProduct().getId(), parameter.getKitchen().getId());

            if (current.compareTo(parameter.getMinStock()) < 0) {
                alerts.add(new LowStockAlertResponse(
                    parameter.getKitchen().getId(), parameter.getProduct().getId(), current, parameter.getMinStock()));
            }
        }

        return alerts;
    }

    @Override
    public List<StockBatch> listByKitchen(Integer kitchenId) {
        return repository.findByKitchenId(kitchenId);
    }

    @Override
    public List<StockBatch> listByProduct(Long productId) {
        return repository.findByProductId(productId);
    }
}

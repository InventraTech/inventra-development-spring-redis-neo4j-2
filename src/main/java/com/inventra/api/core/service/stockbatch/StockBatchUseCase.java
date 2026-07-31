package com.inventra.api.core.service.stockbatch;

import java.math.BigDecimal;
import java.util.List;

import com.inventra.api.core.service.stockbatch.model.request.RegisterStockEntryRequest;
import com.inventra.api.core.service.stockbatch.model.response.LowStockAlertResponse;
import com.inventra.api.core.domain.stock.StockBatch;

public interface StockBatchUseCase {

    StockBatch registerEntry(RegisterStockEntryRequest request);

    StockBatch consume(Integer batchId, BigDecimal quantity);

    // baixa por FEFO (lote mais próximo do vencimento primeiro) — usado pelo fluxo de aprovação de requisição
    void consumeForProduct(Integer kitchenId, Long productId, BigDecimal quantity);

    StockBatch adjust(Integer batchId, BigDecimal newQuantity);

    List<StockBatch> findExpiringSoon(Integer kitchenId, int days);

    List<LowStockAlertResponse> findLowStock();

    List<StockBatch> listByKitchen(Integer kitchenId);

    List<StockBatch> listByProduct(Long productId);
}

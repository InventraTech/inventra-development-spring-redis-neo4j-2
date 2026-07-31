package com.inventra.api.infrastructure.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inventra.api.core.domain.stock.StockBatch;
import com.inventra.api.core.domain.stock.enums.StockBatchStatus;

public interface StockBatchRepository extends JpaRepository<StockBatch, Integer> {

    List<StockBatch> findByKitchenId(Integer kitchenId);

    List<StockBatch> findByProductId(Long productId);

    List<StockBatch> findByKitchenIdAndProductIdAndStatusOrderByExpirationDateAscEntryDateAsc(
            Integer kitchenId, Long productId, StockBatchStatus status);

    List<StockBatch> findByKitchenIdAndStatusAndExpirationDateBetween(
            Integer kitchenId, StockBatchStatus status, LocalDate start, LocalDate end);

    @Query("""
            SELECT COALESCE(SUM(sb.currentQuantity), 0) FROM StockBatch sb
            WHERE sb.product.id = :productId AND sb.kitchen.id = :kitchenId AND sb.status = 'ATIVO'
            """)
    BigDecimal sumActiveQuantity(@Param("productId") Long productId, @Param("kitchenId") Integer kitchenId);

}

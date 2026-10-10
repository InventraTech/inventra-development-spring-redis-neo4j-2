package com.inventra.api.core.service.stockbatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.product.Product;
import com.inventra.api.core.domain.stock.StockBatch;
import com.inventra.api.core.domain.stock.enums.StockBatchStatus;
import com.inventra.api.core.service.stockbatch.model.request.RegisterStockEntryRequest;
import com.inventra.api.infrastructure.exception.BusinessRuleException;
import com.inventra.api.infrastructure.exception.ResourceNotFoundException;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.ProductKitchenParameterRepository;
import com.inventra.api.infrastructure.repository.ProductRepository;
import com.inventra.api.infrastructure.repository.StockBatchRepository;
import com.inventra.api.infrastructure.repository.SupplierRepository;
import com.inventra.api.infrastructure.security.KitchenAccessGuard;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockBatchServiceTest {

    private static final Integer KITCHEN_ID = 1;
    private static final Integer BATCH_ID = 10;

    @Mock private StockBatchRepository repository;
    @Mock private ProductRepository productRepository;
    @Mock private KitchenRepository kitchenRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductKitchenParameterRepository productKitchenParameterRepository;
    @Mock private KitchenAccessGuard accessGuard;

    @InjectMocks private StockBatchService service;

    private Kitchen kitchen;
    private Product product;

    @BeforeEach
    void setUp() {
        kitchen = Kitchen.builder().id(KITCHEN_ID).name("Central").active(true).build();
        product = Product.builder().id(3).name("Arroz").active(true).build();
    }

    private StockBatch batch(String quantity, StockBatchStatus status, LocalDate expiration) {
        return StockBatch.builder().id(BATCH_ID).product(product).kitchen(kitchen)
                .currentQuantity(new BigDecimal(quantity)).initialQuantity(new BigDecimal(quantity))
                .status(status).expirationDate(expiration).build();
    }

    private RegisterStockEntryRequest entry(LocalDate entryDate, LocalDate expiration) {
        return new RegisterStockEntryRequest(3, KITCHEN_ID, null, "L1", null, new BigDecimal("20"),
                entryDate, expiration, new BigDecimal("4.50"));
    }

    // ---------- registerEntry ----------

    @Test
    void registerEntryCreatesActiveBatchWithFullBalance() {
        when(productRepository.findById(3)).thenReturn(Optional.of(product));
        when(kitchenRepository.findById(KITCHEN_ID)).thenReturn(Optional.of(kitchen));
        when(repository.save(any(StockBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        StockBatch result = service.registerEntry(entry(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 1)));

        assertEquals(StockBatchStatus.ACTIVE, result.getStatus());
        assertEquals(new BigDecimal("20"), result.getCurrentQuantity());
        assertEquals(result.getInitialQuantity(), result.getCurrentQuantity());
        verify(accessGuard).assertAccess(KITCHEN_ID);
    }

    @Test
    void registerEntryRejectsExpirationBeforeEntryDate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registerEntry(entry(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1))));
        verify(repository, never()).save(any());
    }

    @Test
    void registerEntryRejectsInactiveProduct() {
        product.setActive(false);
        when(productRepository.findById(3)).thenReturn(Optional.of(product));

        assertThrows(BusinessRuleException.class, () -> service.registerEntry(entry(LocalDate.now(), null)));
        verify(repository, never()).save(any());
    }

    @Test
    void registerEntryRejectsInactiveKitchen() {
        kitchen.setActive(false);
        when(productRepository.findById(3)).thenReturn(Optional.of(product));
        when(kitchenRepository.findById(KITCHEN_ID)).thenReturn(Optional.of(kitchen));

        assertThrows(BusinessRuleException.class, () -> service.registerEntry(entry(LocalDate.now(), null)));
    }

    @Test
    void registerEntryFailsForUnknownProduct() {
        when(productRepository.findById(3)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registerEntry(entry(LocalDate.now(), null)));
    }

    // ---------- consume ----------

    @Test
    void consumeCallsWriteOffProcedure() {
        when(repository.findById(BATCH_ID)).thenReturn(Optional.of(batch("10", StockBatchStatus.ACTIVE, null)));

        service.consume(BATCH_ID, new BigDecimal("4"));

        verify(repository).callWriteOffStock(BATCH_ID, new BigDecimal("4"));
    }

    @Test
    void consumeRejectsQuantityAboveBalance() {
        when(repository.findById(BATCH_ID)).thenReturn(Optional.of(batch("3", StockBatchStatus.ACTIVE, null)));

        assertThrows(BusinessRuleException.class, () -> service.consume(BATCH_ID, new BigDecimal("4")));
        verify(repository, never()).callWriteOffStock(any(), any());
    }

    @Test
    void consumeRejectsExpiredBatch() {
        when(repository.findById(BATCH_ID))
                .thenReturn(Optional.of(batch("10", StockBatchStatus.ACTIVE, LocalDate.now().minusDays(1))));

        assertThrows(BusinessRuleException.class, () -> service.consume(BATCH_ID, BigDecimal.ONE));
        verify(repository, never()).callWriteOffStock(any(), any());
    }

    @Test
    void consumeRejectsNonActiveBatch() {
        when(repository.findById(BATCH_ID)).thenReturn(Optional.of(batch("10", StockBatchStatus.WRITTEN_OFF, null)));

        assertThrows(BusinessRuleException.class, () -> service.consume(BATCH_ID, BigDecimal.ONE));
    }

    @Test
    void consumeFailsForUnknownBatch() {
        when(repository.findById(BATCH_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.consume(BATCH_ID, BigDecimal.ONE));
    }

    // ---------- restock ----------

    @Test
    void restockCallsRegisterEntryProcedure() {
        when(repository.findById(BATCH_ID)).thenReturn(Optional.of(batch("0", StockBatchStatus.WRITTEN_OFF, null)));

        service.restock(BATCH_ID, new BigDecimal("5"));

        verify(repository).callRegisterStockEntry(BATCH_ID, new BigDecimal("5"));
    }

    @Test
    void restockRejectsInactiveKitchen() {
        kitchen.setActive(false);
        when(repository.findById(BATCH_ID)).thenReturn(Optional.of(batch("1", StockBatchStatus.ACTIVE, null)));

        assertThrows(BusinessRuleException.class, () -> service.restock(BATCH_ID, BigDecimal.ONE));
        verify(repository, never()).callRegisterStockEntry(any(), any());
    }

    // ---------- consumeForProduct (FIFO) ----------

    @Test
    void consumeForProductTakesFromBatchesInOrderUntilSatisfied() {
        StockBatch first = batch("3", StockBatchStatus.ACTIVE, null);
        first.setId(11);
        StockBatch second = batch("10", StockBatchStatus.ACTIVE, null);
        second.setId(12);
        when(repository.findUsableForConsumption(eq(KITCHEN_ID), eq(3), any(LocalDate.class)))
                .thenReturn(List.of(first, second));

        service.consumeForProduct(KITCHEN_ID, 3, new BigDecimal("5"));

        verify(repository).callWriteOffStock(11, new BigDecimal("3"));
        verify(repository).callWriteOffStock(12, new BigDecimal("2"));
    }

    @Test
    void consumeForProductFailsWhenStockIsInsufficient() {
        StockBatch only = batch("2", StockBatchStatus.ACTIVE, null);
        when(repository.findUsableForConsumption(eq(KITCHEN_ID), eq(3), any(LocalDate.class)))
                .thenReturn(List.of(only));

        assertThrows(BusinessRuleException.class,
                () -> service.consumeForProduct(KITCHEN_ID, 3, new BigDecimal("5")));
    }

    // ---------- adjust ----------

    @Test
    void adjustToZeroWritesOffTheBatch() {
        StockBatch existing = batch("5", StockBatchStatus.ACTIVE, null);
        when(repository.findByIdForUpdate(BATCH_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(StockBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        StockBatch result = service.adjust(BATCH_ID, BigDecimal.ZERO);

        assertEquals(StockBatchStatus.WRITTEN_OFF, result.getStatus());
    }

    @Test
    void adjustPositiveReactivatesWrittenOffBatch() {
        StockBatch existing = batch("0", StockBatchStatus.WRITTEN_OFF, null);
        when(repository.findByIdForUpdate(BATCH_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(StockBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        StockBatch result = service.adjust(BATCH_ID, new BigDecimal("7"));

        assertEquals(StockBatchStatus.ACTIVE, result.getStatus());
        assertEquals(new BigDecimal("7"), result.getCurrentQuantity());
    }

    @Test
    void adjustFailsForUnknownBatch() {
        when(repository.findByIdForUpdate(BATCH_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.adjust(BATCH_ID, BigDecimal.ONE));
    }

    // ---------- consultas ----------

    @Test
    void findExpiringSoonRejectsNegativeDays() {
        assertThrows(IllegalArgumentException.class, () -> service.findExpiringSoon(KITCHEN_ID, -1));
    }

    @Test
    void findExpiringSoonQueriesActiveBatchesInTheWindow() {
        List<StockBatch> found = List.of(batch("1", StockBatchStatus.ACTIVE, LocalDate.now().plusDays(2)));
        when(repository.findByKitchenIdAndStatusAndExpirationDateBetween(
                eq(KITCHEN_ID), eq(StockBatchStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(found);

        assertSame(found, service.findExpiringSoon(KITCHEN_ID, 7));
    }

    @Test
    void findLowStockIsEmptyForUserWithoutKitchen() {
        when(accessGuard.currentKitchenId()).thenReturn(null);

        assertEquals(List.of(), service.findLowStock());
        verify(productKitchenParameterRepository, never()).findBelowMinimum(any(), any());
    }

    @Test
    void findLowStockIsEmptyWhenNothingIsBelowMinimum() {
        when(accessGuard.currentKitchenId()).thenReturn(KITCHEN_ID);
        when(productKitchenParameterRepository.findBelowMinimum(eq(KITCHEN_ID), any(LocalDate.class)))
                .thenReturn(List.of());

        assertEquals(List.of(), service.findLowStock());
    }

    @Test
    void listByProductIsEmptyForUserWithoutKitchen() {
        when(accessGuard.currentKitchenId()).thenReturn(null);

        assertEquals(List.of(), service.listByProduct(3));
    }
}

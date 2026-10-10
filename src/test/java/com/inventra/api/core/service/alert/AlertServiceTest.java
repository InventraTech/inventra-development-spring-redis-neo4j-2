package com.inventra.api.core.service.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.access.AccessDeniedException;

import com.inventra.api.core.domain.alert.Alert;
import com.inventra.api.core.domain.alert.enums.AlertSeverity;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.product.Product;
import com.inventra.api.core.domain.stock.StockBatch;
import com.inventra.api.core.service.alert.model.request.CreateAlertRequest;
import com.inventra.api.infrastructure.exception.BusinessRuleException;
import com.inventra.api.infrastructure.exception.ResourceNotFoundException;
import com.inventra.api.infrastructure.repository.AlertRepository;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.ProductRepository;
import com.inventra.api.infrastructure.repository.StockBatchRepository;
import com.inventra.api.infrastructure.security.KitchenAccessGuard;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlertServiceTest {

    private static final Integer KITCHEN_ID = 1;

    @Mock private AlertRepository repository;
    @Mock private KitchenRepository kitchenRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockBatchRepository stockBatchRepository;
    @Mock private KitchenAccessGuard accessGuard;

    @InjectMocks private AlertService service;

    private Kitchen kitchen;
    private Product product;

    @BeforeEach
    void setUp() {
        kitchen = Kitchen.builder().id(KITCHEN_ID).name("Central").active(true).build();
        product = Product.builder().id(3).name("Arroz").active(true).build();
        when(kitchenRepository.findById(KITCHEN_ID)).thenReturn(Optional.of(kitchen));
        when(repository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateAlertRequest request(Integer productId, Integer batchId) {
        return new CreateAlertRequest("LOW_STOCK", AlertSeverity.HIGH, batchId, productId, KITCHEN_ID, "Estoque baixo");
    }

    private Alert alert() {
        return Alert.builder().id(5).type("LOW_STOCK").severity(AlertSeverity.HIGH).kitchen(kitchen)
                .message("Estoque baixo").read(false).build();
    }

    // ---------- create ----------

    @Test
    void createSavesUnreadAlert() {
        Alert result = service.create(request(null, null));

        assertFalse(result.getRead());
        assertEquals(AlertSeverity.HIGH, result.getSeverity());
        assertSame(kitchen, result.getKitchen());
        verify(accessGuard).assertAccess(KITCHEN_ID);
    }

    @Test
    void createLinksProductAndBatchOfTheSameKitchen() {
        StockBatch batch = StockBatch.builder().id(10).kitchen(kitchen).product(product).build();
        when(productRepository.findById(3)).thenReturn(Optional.of(product));
        when(stockBatchRepository.findById(10)).thenReturn(Optional.of(batch));

        Alert result = service.create(request(3, 10));

        assertSame(product, result.getProduct());
        assertSame(batch, result.getBatch());
    }

    @Test
    void createRejectsBatchFromAnotherKitchen() {
        StockBatch foreign = StockBatch.builder().id(10).kitchen(Kitchen.builder().id(99).build()).product(product).build();
        when(stockBatchRepository.findById(10)).thenReturn(Optional.of(foreign));

        assertThrows(BusinessRuleException.class, () -> service.create(request(null, 10)));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsBatchThatBelongsToAnotherProduct() {
        Product other = Product.builder().id(4).build();
        StockBatch batch = StockBatch.builder().id(10).kitchen(kitchen).product(other).build();
        when(productRepository.findById(3)).thenReturn(Optional.of(product));
        when(stockBatchRepository.findById(10)).thenReturn(Optional.of(batch));

        assertThrows(BusinessRuleException.class, () -> service.create(request(3, 10)));
    }

    @Test
    void createFailsForUnknownProductBatchOrKitchen() {
        when(productRepository.findById(3)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(request(3, null)));

        when(stockBatchRepository.findById(10)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(request(null, 10)));

        when(kitchenRepository.findById(KITCHEN_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(request(null, null)));
    }

    @Test
    void createIsBlockedWithoutAccessToKitchen() {
        doThrow(new AccessDeniedException("sem acesso")).when(accessGuard).assertAccess(KITCHEN_ID);

        assertThrows(AccessDeniedException.class, () -> service.create(request(null, null)));
        verify(repository, never()).save(any());
    }

    // ---------- leitura ----------

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(404)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(404));
    }

    @Test
    void findByIdChecksAccessToTheAlertKitchen() {
        when(repository.findById(5)).thenReturn(Optional.of(alert()));
        doThrow(new AccessDeniedException("sem acesso")).when(accessGuard).assertAccess(KITCHEN_ID);

        assertThrows(AccessDeniedException.class, () -> service.findById(5));
    }

    @Test
    void listByKitchenAndListUnreadUseTheirOwnQueries() {
        List<Alert> all = List.of(alert());
        List<Alert> unread = List.of(alert());
        when(repository.findByKitchenId(KITCHEN_ID)).thenReturn(all);
        when(repository.findByKitchenIdAndReadFalse(KITCHEN_ID)).thenReturn(unread);

        assertSame(all, service.listByKitchen(KITCHEN_ID));
        assertSame(unread, service.listUnreadByKitchen(KITCHEN_ID));
    }

    // ---------- markAsRead / delete ----------

    @Test
    void markAsReadSetsTheFlag() {
        when(repository.findById(5)).thenReturn(Optional.of(alert()));

        Alert result = service.markAsRead(5);

        assertTrue(result.getRead());
    }

    @Test
    void deleteRemovesAccessibleAlert() {
        Alert existing = alert();
        when(repository.findById(5)).thenReturn(Optional.of(existing));

        service.delete(5);

        verify(repository).delete(existing);
    }

    @Test
    void deleteIsBlockedWithoutAccess() {
        when(repository.findById(5)).thenReturn(Optional.of(alert()));
        doThrow(new AccessDeniedException("sem acesso")).when(accessGuard).assertAccess(KITCHEN_ID);

        assertThrows(AccessDeniedException.class, () -> service.delete(5));
        verify(repository, never()).delete(any());
    }
}

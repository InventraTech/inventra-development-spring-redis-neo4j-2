package com.inventra.api.core.service.requisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.product.Product;
import com.inventra.api.core.domain.requisition.Requisition;
import com.inventra.api.core.domain.requisition.RequisitionItem;
import com.inventra.api.core.domain.requisition.enums.RequisitionStatus;
import com.inventra.api.core.domain.requisition.enums.RequisitionType;
import com.inventra.api.core.domain.user.User;
import com.inventra.api.core.service.requisition.model.request.AddRequisitionItemRequest;
import com.inventra.api.core.service.requisition.model.request.CreateRequisitionRequest;
import com.inventra.api.core.service.stockbatch.StockBatchUseCase;
import com.inventra.api.infrastructure.exception.BusinessRuleException;
import com.inventra.api.infrastructure.exception.ResourceNotFoundException;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.ProductRepository;
import com.inventra.api.infrastructure.repository.ProductSupplierRepository;
import com.inventra.api.infrastructure.repository.RequisitionItemRepository;
import com.inventra.api.infrastructure.repository.RequisitionRepository;
import com.inventra.api.infrastructure.repository.SupplierRepository;
import com.inventra.api.infrastructure.repository.UserRepository;
import com.inventra.api.infrastructure.security.KitchenAccessGuard;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequisitionServiceTest {

    private static final Integer KITCHEN_ID = 1;
    private static final Integer REQ_ID = 5;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private RequisitionRepository repository;
    @Mock private RequisitionItemRepository itemRepository;
    @Mock private KitchenRepository kitchenRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductSupplierRepository productSupplierRepository;
    @Mock private StockBatchUseCase stockBatchUseCase;
    @Mock private KitchenAccessGuard accessGuard;

    @InjectMocks private RequisitionService service;

    private Kitchen kitchen;
    private User requester;

    @BeforeEach
    void setUp() {
        kitchen = Kitchen.builder().id(KITCHEN_ID).name("Central").active(true).build();
        requester = User.builder().id(USER_ID).name("Maria").build();
        when(accessGuard.currentUser()).thenReturn(requester);
        when(accessGuard.isSupervisor()).thenReturn(false);
    }

    private Requisition requisition(RequisitionStatus status, RequisitionType type) {
        return Requisition.builder().id(REQ_ID).type(type).origin("Central").status(status)
                .kitchen(kitchen).requester(requester).build();
    }

    private void stubRequisition(Requisition requisition) {
        when(repository.findById(REQ_ID)).thenReturn(Optional.of(requisition));
    }

    // ---------- create ----------

    @Test
    void createOpensRequisitionUnderReviewForLoggedUser() {
        when(kitchenRepository.findById(KITCHEN_ID)).thenReturn(Optional.of(kitchen));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(requester));
        when(repository.save(any(Requisition.class))).thenAnswer(inv -> inv.getArgument(0));

        Requisition result = service.create(new CreateRequisitionRequest(RequisitionType.PURCHASE, "Central", KITCHEN_ID));

        assertEquals(RequisitionStatus.UNDER_REVIEW, result.getStatus());
        assertSame(requester, result.getRequester());
        verify(accessGuard).assertAccess(KITCHEN_ID);
    }

    @Test
    void createRejectsInactiveKitchen() {
        kitchen.setActive(false);
        when(kitchenRepository.findById(KITCHEN_ID)).thenReturn(Optional.of(kitchen));

        assertThrows(BusinessRuleException.class,
                () -> service.create(new CreateRequisitionRequest(RequisitionType.PURCHASE, "Central", KITCHEN_ID)));
        verify(repository, never()).save(any());
    }

    @Test
    void createFailsWhenKitchenDoesNotExist() {
        when(kitchenRepository.findById(KITCHEN_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(new CreateRequisitionRequest(RequisitionType.PURCHASE, "Central", KITCHEN_ID)));
    }

    @Test
    void createIsBlockedWhenUserHasNoAccessToKitchen() {
        doThrow(new AccessDeniedException("sem acesso")).when(accessGuard).assertAccess(KITCHEN_ID);

        assertThrows(AccessDeniedException.class,
                () -> service.create(new CreateRequisitionRequest(RequisitionType.PURCHASE, "Central", KITCHEN_ID)));
        verify(repository, never()).save(any());
    }

    // ---------- addItem ----------

    @Test
    void addItemSavesItemWithGivenPrice() {
        stubRequisition(requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE));
        Product product = Product.builder().id(3).name("Arroz").active(true).build();
        when(productRepository.findById(3)).thenReturn(Optional.of(product));

        service.addItem(REQ_ID, new AddRequisitionItemRequest(3, new BigDecimal("2"), null, new BigDecimal("9.90"), "urgente"));

        verify(itemRepository).save(any(RequisitionItem.class));
    }

    @Test
    void addItemRejectsInactiveProduct() {
        stubRequisition(requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE));
        when(productRepository.findById(3)).thenReturn(Optional.of(Product.builder().id(3).active(false).build()));

        assertThrows(BusinessRuleException.class,
                () -> service.addItem(REQ_ID, new AddRequisitionItemRequest(3, BigDecimal.ONE, null, null, null)));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void addItemRejectsRequisitionNoLongerUnderReview() {
        stubRequisition(requisition(RequisitionStatus.APPROVED, RequisitionType.PURCHASE));

        assertThrows(BusinessRuleException.class,
                () -> service.addItem(REQ_ID, new AddRequisitionItemRequest(3, BigDecimal.ONE, null, null, null)));
    }

    @Test
    void addItemIsBlockedForAnotherUsersRequisitionWhenNotSupervisor() {
        Requisition other = requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE);
        other.setRequester(User.builder().id(UUID.randomUUID()).build());
        stubRequisition(other);

        assertThrows(AccessDeniedException.class,
                () -> service.addItem(REQ_ID, new AddRequisitionItemRequest(3, BigDecimal.ONE, null, null, null)));
    }

    // ---------- removeItem ----------

    @Test
    void removeItemDeletesItemOfTheRequisition() {
        Requisition requisition = requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE);
        stubRequisition(requisition);
        RequisitionItem item = RequisitionItem.builder().id(8).requisition(requisition).build();
        when(itemRepository.findById(8)).thenReturn(Optional.of(item));

        service.removeItem(REQ_ID, 8);

        verify(itemRepository).delete(item);
    }

    @Test
    void removeItemRejectsItemFromAnotherRequisition() {
        stubRequisition(requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE));
        Requisition another = Requisition.builder().id(99).kitchen(kitchen).requester(requester).build();
        when(itemRepository.findById(8)).thenReturn(Optional.of(RequisitionItem.builder().id(8).requisition(another).build()));

        assertThrows(BusinessRuleException.class, () -> service.removeItem(REQ_ID, 8));
        verify(itemRepository, never()).delete(any());
    }

    // ---------- submit ----------

    @Test
    void submitRejectsRequisitionWithoutItems() {
        stubRequisition(requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE));
        when(itemRepository.countByRequisitionId(REQ_ID)).thenReturn(0L);

        assertThrows(BusinessRuleException.class, () -> service.submit(REQ_ID));
    }

    @Test
    void submitAcceptsRequisitionWithItems() {
        Requisition requisition = requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE);
        stubRequisition(requisition);
        when(itemRepository.countByRequisitionId(REQ_ID)).thenReturn(2L);

        assertSame(requisition, service.submit(REQ_ID));
    }

    // ---------- approve ----------

    @Test
    void approveConsumptionCallsProcedureAndConsumesStock() {
        stubRequisition(requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.CONSUMPTION));
        when(itemRepository.countByRequisitionId(REQ_ID)).thenReturn(1L);
        Product product = Product.builder().id(3).active(true).build();
        when(itemRepository.findByRequisitionId(REQ_ID)).thenReturn(List.of(
                RequisitionItem.builder().id(1).product(product).quantity(new BigDecimal("4")).build()));

        service.approve(REQ_ID);

        verify(repository).callApproveRequisition(REQ_ID, USER_ID);
        verify(stockBatchUseCase).consumeForProduct(KITCHEN_ID, 3, new BigDecimal("4"));
    }

    @Test
    void approvePurchaseDoesNotTouchStock() {
        stubRequisition(requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE));
        when(itemRepository.countByRequisitionId(REQ_ID)).thenReturn(1L);

        service.approve(REQ_ID);

        verify(repository).callApproveRequisition(REQ_ID, USER_ID);
        verify(stockBatchUseCase, never()).consumeForProduct(any(), any(), any());
    }

    @Test
    void approveRejectsRequisitionWithoutItems() {
        stubRequisition(requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE));
        when(itemRepository.countByRequisitionId(REQ_ID)).thenReturn(0L);

        assertThrows(BusinessRuleException.class, () -> service.approve(REQ_ID));
        verify(repository, never()).callApproveRequisition(any(), any());
    }

    @Test
    void approveRejectsAlreadyDecidedRequisition() {
        stubRequisition(requisition(RequisitionStatus.REJECTED, RequisitionType.PURCHASE));

        assertThrows(BusinessRuleException.class, () -> service.approve(REQ_ID));
        verify(repository, never()).callApproveRequisition(any(), any());
    }

    // ---------- reject ----------

    @Test
    void rejectCallsProcedureWithLoggedUserAndReason() {
        stubRequisition(requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE));

        service.reject(REQ_ID, "Fora do orçamento");

        verify(repository).callRejectRequisition(REQ_ID, USER_ID, "Fora do orçamento");
    }

    @Test
    void rejectRejectsRequisitionNoLongerUnderReview() {
        stubRequisition(requisition(RequisitionStatus.CANCELLED, RequisitionType.PURCHASE));

        assertThrows(BusinessRuleException.class, () -> service.reject(REQ_ID, "x"));
        verify(repository, never()).callRejectRequisition(any(), any(), any());
    }

    // ---------- cancel ----------

    @Test
    void cancelUnderReviewByOwnerCallsProcedure() {
        stubRequisition(requisition(RequisitionStatus.UNDER_REVIEW, RequisitionType.PURCHASE));

        service.cancel(REQ_ID, "Desisti");

        verify(repository).callCancelRequisition(REQ_ID, "Desisti");
    }

    @Test
    void cancelApprovedRequisitionIsBlockedForNonSupervisor() {
        stubRequisition(requisition(RequisitionStatus.APPROVED, RequisitionType.CONSUMPTION));

        assertThrows(AccessDeniedException.class, () -> service.cancel(REQ_ID, "Erro"));
        verify(repository, never()).callCancelRequisition(any(), any());
    }

    @Test
    void cancelApprovedRequisitionIsAllowedForSupervisor() {
        when(accessGuard.isSupervisor()).thenReturn(true);
        stubRequisition(requisition(RequisitionStatus.APPROVED, RequisitionType.CONSUMPTION));

        service.cancel(REQ_ID, "Erro");

        verify(repository).callCancelRequisition(REQ_ID, "Erro");
    }

    // ---------- listagens ----------

    @Test
    void listByStatusIsEmptyForUserWithoutKitchen() {
        when(accessGuard.currentKitchenId()).thenReturn(null);

        assertEquals(List.of(), service.listByStatus(RequisitionStatus.APPROVED));
        verify(repository, never()).findByKitchenIdAndStatus(any(), any());
    }

    @Test
    void listByStatusScopesToUserKitchen() {
        when(accessGuard.currentKitchenId()).thenReturn(KITCHEN_ID);
        List<Requisition> found = List.of(requisition(RequisitionStatus.APPROVED, RequisitionType.PURCHASE));
        when(repository.findByKitchenIdAndStatus(KITCHEN_ID, RequisitionStatus.APPROVED)).thenReturn(found);

        assertSame(found, service.listByStatus(RequisitionStatus.APPROVED));
    }

    @Test
    void findingMissingRequisitionThrowsNotFound() {
        when(repository.findById(404)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listItems(404));
    }
}

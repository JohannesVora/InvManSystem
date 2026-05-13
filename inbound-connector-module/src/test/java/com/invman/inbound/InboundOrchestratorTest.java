package com.invman.inbound;

import com.invman.common.connector.PosInvoicePayload;
import com.invman.common.entity.InventoryItem;
import com.invman.common.entity.InventoryTransaction;
import com.invman.common.entity.ProductComponent;
import com.invman.common.entity.SalesProduct;
import com.invman.common.enums.TransactionType;
import com.invman.common.repository.AppSettingRepository;
import com.invman.common.repository.InventoryTransactionRepository;
import com.invman.common.repository.ProductComponentRepository;
import com.invman.common.repository.SalesProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundOrchestratorTest {

    @Mock SalesProductRepository salesProductRepository;
    @Mock ProductComponentRepository productComponentRepository;
    @Mock InventoryTransactionRepository inventoryTransactionRepository;
    @Mock AppSettingRepository appSettingRepository;

    @InjectMocks
    InboundOrchestrator orchestrator;

    @Test
    void processInvoices_validInvoice_createsTransaction() {
        SalesProduct sp = new SalesProduct();
        sp.setId(1L);

        InventoryItem item = new InventoryItem();
        item.setId(10L);
        item.setName("Cola 0.33L");

        ProductComponent component = new ProductComponent();
        component.setInventoryItem(item);
        component.setQtyRequired(1.0);

        when(inventoryTransactionRepository.existsByReferenceId("INV-001")).thenReturn(false);
        when(salesProductRepository.findByExternalIdAndPosSystem("10001", "READY2ORDER"))
                .thenReturn(Optional.of(sp));
        when(productComponentRepository.findBySalesProductId(1L))
                .thenReturn(List.of(component));
        when(inventoryTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PosInvoicePayload invoice = new PosInvoicePayload(
                "INV-001", "READY2ORDER",
                List.of(new PosInvoicePayload.LineItem("10001", 2.0))
        );

        orchestrator.processInvoices(List.of(invoice));

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventoryTransactionRepository).save(captor.capture());
        InventoryTransaction tx = captor.getValue();
        assertThat(tx.getDelta()).isEqualTo(-2.0);
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.POS_SALE);
        assertThat(tx.getReferenceId()).isEqualTo("INV-001");
    }

    @Test
    void processInvoices_duplicateInvoice_skipped() {
        when(inventoryTransactionRepository.existsByReferenceId("INV-002")).thenReturn(true);

        PosInvoicePayload invoice = new PosInvoicePayload(
                "INV-002", "READY2ORDER",
                List.of(new PosInvoicePayload.LineItem("10001", 1.0))
        );

        orchestrator.processInvoices(List.of(invoice));

        verify(inventoryTransactionRepository, never()).save(any());
        verify(salesProductRepository, never()).findByExternalIdAndPosSystem(anyString(), anyString());
    }

    @Test
    void processInvoices_unknownProduct_skippedGracefully() {
        when(inventoryTransactionRepository.existsByReferenceId("INV-003")).thenReturn(false);
        when(salesProductRepository.findByExternalIdAndPosSystem("99999", "READY2ORDER"))
                .thenReturn(Optional.empty());

        PosInvoicePayload invoice = new PosInvoicePayload(
                "INV-003", "READY2ORDER",
                List.of(new PosInvoicePayload.LineItem("99999", 1.0))
        );

        orchestrator.processInvoices(List.of(invoice));

        verify(inventoryTransactionRepository, never()).save(any());
    }
}

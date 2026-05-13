package com.invman.inbound;

import com.invman.common.connector.PosInvoicePayload;
import com.invman.common.entity.InventoryTransaction;
import com.invman.common.entity.ProductComponent;
import com.invman.common.entity.SalesProduct;
import com.invman.common.enums.TransactionType;
import com.invman.common.repository.AppSettingRepository;
import com.invman.common.repository.InventoryItemRepository;
import com.invman.common.repository.InventoryTransactionRepository;
import com.invman.common.repository.ProductComponentRepository;
import com.invman.common.repository.SalesProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundOrchestrator {

    private final SalesProductRepository salesProductRepository;
    private final ProductComponentRepository productComponentRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final AppSettingRepository appSettingRepository;

    @Transactional
    public void processInvoices(List<PosInvoicePayload> invoices) {
        for (PosInvoicePayload invoice : invoices) {
            try {
                processInvoice(invoice);
            } catch (Exception e) {
                log.error("Failed to process invoice {}: {}", invoice.invoiceId(), e.getMessage());
            }
        }
        if (!invoices.isEmpty()) {
            appSettingRepository.upsert("r2o.lastPolledAt", LocalDateTime.now().toString());
        }
    }

    private void processInvoice(PosInvoicePayload invoice) {
        if (inventoryTransactionRepository.existsByReferenceId(invoice.invoiceId())) {
            log.info("Invoice {} already processed, skipping duplicate", invoice.invoiceId());
            return;
        }

        for (PosInvoicePayload.LineItem lineItem : invoice.items()) {
            Optional<SalesProduct> spOpt = salesProductRepository
                    .findByExternalIdAndPosSystem(lineItem.productId(), invoice.posSource());
            if (spOpt.isEmpty()) {
                log.info("No SalesProduct mapped for externalId={} posSystem={}, skipping line item",
                        lineItem.productId(), invoice.posSource());
                continue;
            }

            List<ProductComponent> components = productComponentRepository
                    .findBySalesProductId(spOpt.get().getId());
            for (ProductComponent component : components) {
                InventoryTransaction tx = new InventoryTransaction();
                tx.setInventoryItem(component.getInventoryItem());
                tx.setTransactionType(TransactionType.POS_SALE);
                tx.setDelta(-component.getQtyRequired() * lineItem.quantitySold());
                tx.setReferenceId(invoice.invoiceId());
                tx.setCreatedAt(LocalDateTime.now());
                inventoryTransactionRepository.save(tx);
                inventoryItemRepository.recalculateCachedStock(component.getInventoryItem().getId());
                log.info("Invoice {} — deducted {} {} from inventory item '{}' (stock recalculated)",
                        invoice.invoiceId(), tx.getDelta(), component.getInventoryItem().getUnit(),
                        component.getInventoryItem().getName());
            }
        }
    }
}

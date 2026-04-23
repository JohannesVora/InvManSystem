package com.invman.app;

import com.invman.common.entity.*;
import com.invman.common.enums.ConnectorDirection;
import com.invman.common.enums.ReplenishmentOrderStatus;
import com.invman.common.repository.*;
import com.invman.outbound.ConnectorOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConnectorOrchestratorIntegrationTest {

    @Autowired ConnectorOrchestrator orchestrator;
    @Autowired ReplenishmentOrderRepository orderRepository;
    @Autowired ReplenishmentOrderLineRepository lineRepository;
    @Autowired InventoryItemRepository inventoryItemRepository;
    @Autowired SupplierRepository supplierRepository;
    @Autowired SupplierProductOfferRepository offerRepository;
    @Autowired ConnectorTypeRepository connectorTypeRepository;
    @Autowired ConnectorConfigRepository connectorConfigRepository;

    @MockBean
    JavaMailSenderImpl mailSender;

    private InventoryItem inventoryItem;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        doNothing().when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        supplier = new Supplier();
        supplier.setName("Integration Test Supplier");
        supplier.setContactEmail("test@supplier.com");
        supplier = supplierRepository.save(supplier);

        inventoryItem = new InventoryItem();
        inventoryItem.setName("Test Flour");
        inventoryItem.setUnit("kg");
        inventoryItem.setCachedStock(5.0);
        inventoryItem.setMinStockLevel(10.0);
        inventoryItem.setReorderTarget(50.0);
        inventoryItem = inventoryItemRepository.save(inventoryItem);

        SupplierProductOffer offer = new SupplierProductOffer();
        offer.setSupplier(supplier);
        offer.setInventoryItem(inventoryItem);
        offer.setConversionFactor(25.0);
        offer.setIsPreferred(true);
        offer.setSupplierSku("TEST-25KG");
        offerRepository.save(offer);

        ConnectorType connectorType = connectorTypeRepository.findByName("EMAIL")
                .orElseGet(() -> {
                    ConnectorType ct = new ConnectorType();
                    ct.setName("EMAIL");
                    ct.setDirection(ConnectorDirection.OUTBOUND);
                    return connectorTypeRepository.save(ct);
                });

        ConnectorConfig config = new ConnectorConfig();
        config.setSupplier(supplier);
        config.setConnectorType(connectorType);
        config.setConfigPayload("{\"recipientEmail\":\"orders@supplier.com\",\"subjectTemplate\":\"Order {date}\",\"bodyTemplate\":\"{orderLines}\"}");
        config.setIsActive(true);
        connectorConfigRepository.save(config);
    }

    @Test
    void processesReplenishmentOrderAndSubmits() {
        ReplenishmentOrder order = new ReplenishmentOrder();
        order.setStatus(ReplenishmentOrderStatus.DRAFT);
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        ReplenishmentOrderLine line = new ReplenishmentOrderLine();
        line.setReplenishmentOrder(order);
        line.setInventoryItem(inventoryItem);
        line.setRequestedQty(30.0);
        lineRepository.save(line);

        order.setLines(new ArrayList<>(List.of(line)));
        orderRepository.save(order);

        orchestrator.process(order.getId());

        ReplenishmentOrder processed = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(ReplenishmentOrderStatus.SUBMITTED);
        assertThat(processed.getSubmittedAt()).isNotNull();
    }
}

package com.invman.outbound;

import com.invman.common.connector.ConnectorResult;
import com.invman.common.connector.OrderProcessor;
import com.invman.common.connector.OutboundConnector;
import com.invman.common.connector.SupplierOrderPayload;
import com.invman.common.entity.*;
import com.invman.common.enums.ConnectorExecutionStatus;
import com.invman.common.enums.ReplenishmentOrderStatus;
import com.invman.common.enums.SupplierOrderStatus;
import com.invman.common.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectorOrchestrator implements OrderProcessor {

    private final ReplenishmentOrderRepository replenishmentOrderRepository;
    private final SupplierOrderRepository supplierOrderRepository;
    private final SupplierOrderLineRepository supplierOrderLineRepository;
    private final ConnectorConfigRepository connectorConfigRepository;
    private final ConnectorExecutionRepository connectorExecutionRepository;
    private final SupplierOrderSplitter splitter;
    private final ConnectorRegistry connectorRegistry;
    private final StatusAggregator statusAggregator;

    @Transactional
    public void process(Long replenishmentOrderId) {
        ReplenishmentOrder replenishmentOrder = replenishmentOrderRepository.findById(replenishmentOrderId)
                .orElseThrow(() -> new IllegalArgumentException("ReplenishmentOrder not found: " + replenishmentOrderId));

        List<SupplierOrderSplitter.SupplierSplit> splits = splitter.split(replenishmentOrder.getLines());

        for (SupplierOrderSplitter.SupplierSplit split : splits) {
            SupplierOrder supplierOrder = createSupplierOrder(replenishmentOrder, split);
            List<ConnectorResult> results = transmit(supplierOrder, split);
            SupplierOrderStatus aggregated = statusAggregator.aggregate(results);
            supplierOrder.setStatus(aggregated);
            supplierOrderRepository.save(supplierOrder);
        }

        replenishmentOrder.setStatus(ReplenishmentOrderStatus.SUBMITTED);
        replenishmentOrder.setSubmittedAt(LocalDateTime.now());
        replenishmentOrderRepository.save(replenishmentOrder);
    }

    private SupplierOrder createSupplierOrder(ReplenishmentOrder replenishmentOrder,
                                               SupplierOrderSplitter.SupplierSplit split) {
        log.debug("→ createSupplierOrder(replenishmentOrderId={}, supplier={})",
                replenishmentOrder.getId(), split.supplier().getName());
        SupplierOrder supplierOrder = new SupplierOrder();
        supplierOrder.setReplenishmentOrder(replenishmentOrder);
        supplierOrder.setSupplier(split.supplier());
        supplierOrder.setStatus(SupplierOrderStatus.PENDING);
        supplierOrder.setCreatedAt(LocalDateTime.now());
        supplierOrder = supplierOrderRepository.save(supplierOrder);

        List<SupplierOrderLine> orderLines = new ArrayList<>();
        for (SupplierOrderSplitter.SplitLine sl : split.lines()) {
            SupplierOrderLine line = new SupplierOrderLine();
            line.setSupplierOrder(supplierOrder);
            line.setInventoryItem(sl.orderLine().getInventoryItem());
            line.setOrderedQty(sl.orderedQty());
            line.setSupplierSku(sl.offer().getSupplierSku());
            orderLines.add(supplierOrderLineRepository.save(line));
        }
        supplierOrder.setLines(orderLines);
        log.debug("← createSupplierOrder: created SupplierOrder id={}", supplierOrder.getId());
        return supplierOrder;
    }

    private List<ConnectorResult> transmit(SupplierOrder supplierOrder,
                                            SupplierOrderSplitter.SupplierSplit split) {
        log.debug("→ transmit(supplierOrderId={}, supplier={})",
                supplierOrder.getId(), split.supplier().getName());
        List<ConnectorResult> results = new ArrayList<>();

        log.debug("  looking up active ConnectorConfig for supplierId={}", split.supplier().getId());
        ConnectorConfig config = connectorConfigRepository
                .findBySupplierIdAndIsActiveTrue(split.supplier().getId())
                .orElse(null);

        if (config == null) {
            log.warn("No active connector config for supplier {}", split.supplier().getId());
            return results;
        }
        log.debug("  found ConnectorConfig id={}, type={}", config.getId(), config.getConnectorType().getName());

        String connectorTypeName = config.getConnectorType().getName();
        if (!connectorRegistry.hasConnector(connectorTypeName)) {
            log.warn("No connector registered for type: {}", connectorTypeName);
            return results;
        }

        OutboundConnector connector = connectorRegistry.getConnector(connectorTypeName);
        log.debug("  resolved connector: {} ({})", connectorTypeName, connector.getClass().getSimpleName());

        SupplierOrderPayload payload = buildPayload(supplierOrder, split);

        ConnectorResult result;
        log.debug("  calling {}.transmit() for supplierOrderId={}", connector.getClass().getSimpleName(), supplierOrder.getId());
        try {
            result = connector.transmit(payload, config);
        } catch (Exception e) {
            log.error("  {}.transmit() threw {}: {}", connector.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
            result = ConnectorResult.failure(e.getMessage());
        }
        log.debug("  {}.transmit() returned: success={}, message='{}'",
                connector.getClass().getSimpleName(), result.success(), result.message());

        results.add(result);
        recordExecution(supplierOrder, config, result);
        log.debug("← transmit: {} result(s)", results.size());
        return results;
    }

    private SupplierOrderPayload buildPayload(SupplierOrder supplierOrder,
                                               SupplierOrderSplitter.SupplierSplit split) {
        log.debug("→ buildPayload(supplierOrderId={}, lineCount={})",
                supplierOrder.getId(), split.lines().size());
        List<SupplierOrderPayload.OrderLineItem> lineItems = split.lines().stream()
                .map(sl -> new SupplierOrderPayload.OrderLineItem(
                        sl.orderLine().getInventoryItem().getName(),
                        sl.offer().getSupplierSku(),
                        sl.orderedQty(),
                        sl.orderLine().getInventoryItem().getUnit()
                ))
                .toList();

        SupplierOrderPayload result = new SupplierOrderPayload(
                supplierOrder.getId(),
                split.supplier().getName(),
                LocalDate.now().toString(),
                lineItems
        );
        log.debug("← buildPayload: payload for supplier '{}', {} lines", result.supplierName(), result.lines().size());
        result.lines().forEach(l -> log.debug("    line: {} (SKU: {}) x {} {}",
                l.inventoryItemName(), l.supplierSku(), l.orderedQty(), l.unit()));
        return result;
    }

    private void recordExecution(SupplierOrder supplierOrder, ConnectorConfig config, ConnectorResult result) {
        log.debug("→ recordExecution(supplierOrderId={}, status={})",
                supplierOrder.getId(), result.success() ? "SUCCESS" : "FAILED");
        ConnectorExecution execution = new ConnectorExecution();
        execution.setSupplierOrder(supplierOrder);
        execution.setConnectorConfig(config);
        execution.setStatus(result.success()
                ? ConnectorExecutionStatus.SUCCESS
                : ConnectorExecutionStatus.FAILED);
        execution.setErrorMessage(result.success() ? null : result.message());
        execution.setExecutedAt(LocalDateTime.now());
        connectorExecutionRepository.save(execution);
        log.debug("← recordExecution: saved ConnectorExecution id={}", execution.getId());
    }
}

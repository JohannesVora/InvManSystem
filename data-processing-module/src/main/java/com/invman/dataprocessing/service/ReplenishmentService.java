package com.invman.dataprocessing.service;

import com.invman.common.dto.GoodsReceiptLineDto;
import com.invman.common.dto.GoodsReceiptRequestDto;
import com.invman.common.dto.ReplenishmentOrderDetailDto;
import com.invman.common.dto.ReplenishmentOrderLineDto;
import com.invman.common.dto.ReplenishmentOrderRequest;
import com.invman.common.entity.InventoryItem;
import com.invman.common.entity.InventoryTransaction;
import com.invman.common.entity.ReplenishmentOrder;
import com.invman.common.entity.ReplenishmentOrderLine;
import com.invman.common.enums.ReplenishmentOrderStatus;
import com.invman.common.enums.TransactionType;
import com.invman.common.entity.SupplierProductOffer;
import com.invman.common.repository.InventoryItemRepository;
import com.invman.common.repository.InventoryTransactionRepository;
import com.invman.common.repository.ReplenishmentOrderRepository;
import com.invman.common.repository.SupplierProductOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplenishmentService {

    private final ReplenishmentOrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final StockCalculator stockCalculator;
    private final SupplierProductOfferRepository offerRepository;

    @Transactional
    public ReplenishmentOrderDetailDto createOrder(ReplenishmentOrderRequest request) {
        ReplenishmentOrder order = new ReplenishmentOrder();
        order.setStatus(ReplenishmentOrderStatus.DRAFT);
        order.setCreatedAt(LocalDateTime.now());

        List<ReplenishmentOrderLine> lines = request.lines().stream()
                .map(line -> {
                    InventoryItem item = inventoryItemRepository.findById(line.inventoryItemId())
                            .orElseThrow(() -> new IllegalArgumentException("InventoryItem not found: " + line.inventoryItemId()));
                    ReplenishmentOrderLine ol = new ReplenishmentOrderLine();
                    ol.setReplenishmentOrder(order);
                    ol.setInventoryItem(item);
                    ol.setRequestedQty(line.requestedQty());
                    return ol;
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        order.setLines(lines);
        ReplenishmentOrder saved = orderRepository.save(order);
        log.info("Created replenishment order #{} with {} line(s)", saved.getId(), saved.getLines().size());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ReplenishmentOrderDetailDto getOrder(Long id) {
        ReplenishmentOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<ReplenishmentOrderDetailDto> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ReplenishmentOrderDetailDto bookGoodsReceipt(Long orderId, GoodsReceiptRequestDto req) {
        ReplenishmentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() == ReplenishmentOrderStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already received");
        }
        for (GoodsReceiptLineDto lineDto : req.lines()) {
            if (lineDto.receivedQty() != null && lineDto.receivedQty() > 0) {
                InventoryItem item = inventoryItemRepository.findById(lineDto.inventoryItemId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "InventoryItem not found: " + lineDto.inventoryItemId()));
                InventoryTransaction tx = new InventoryTransaction();
                tx.setInventoryItem(item);
                tx.setTransactionType(TransactionType.GOODS_RECEIPT);
                tx.setDelta(lineDto.receivedQty());
                tx.setReferenceId("ORDER-" + orderId);
                tx.setCreatedAt(LocalDateTime.now());
                transactionRepository.save(tx);
                stockCalculator.recalculate(lineDto.inventoryItemId());
                log.info("Goods receipt order #{}: +{} {} for '{}'",
                        orderId, lineDto.receivedQty(), item.getUnit(), item.getName());
                order.getLines().stream()
                        .filter(l -> l.getInventoryItem().getId().equals(lineDto.inventoryItemId()))
                        .findFirst()
                        .ifPresent(l -> l.setReceivedQty(lineDto.receivedQty()));
            }
        }
        order.setStatus(ReplenishmentOrderStatus.RECEIVED);
        order.setReceivedAt(LocalDateTime.now());
        log.info("Goods receipt booked for order #{} — status set to RECEIVED", orderId);
        return toDto(orderRepository.save(order));
    }

    private ReplenishmentOrderDetailDto toDto(ReplenishmentOrder order) {
        List<ReplenishmentOrderLineDto> lines = order.getLines().stream()
                .map(l -> {
                    SupplierProductOffer offer = offerRepository
                            .findByInventoryItemIdAndIsPreferredTrue(l.getInventoryItem().getId())
                            .orElse(null);
                    return new ReplenishmentOrderLineDto(
                            l.getId(),
                            l.getInventoryItem().getId(),
                            l.getInventoryItem().getName(),
                            l.getInventoryItem().getUnit(),
                            l.getRequestedQty(),
                            l.getReceivedQty(),
                            offer != null ? offer.getSupplierSku() : null,
                            offer != null ? offer.getPackageUnit() : null,
                            offer != null ? offer.getConversionFactor() : null);
                })
                .toList();
        return new ReplenishmentOrderDetailDto(
                order.getId(), order.getStatus(),
                order.getCreatedAt(), order.getSubmittedAt(), order.getReceivedAt(),
                lines);
    }
}

package com.invman.dataprocessing.service;

import com.invman.common.dto.ReplenishmentOrderRequest;
import com.invman.common.dto.ReplenishmentOrderStatusDto;
import com.invman.common.entity.InventoryItem;
import com.invman.common.entity.ReplenishmentOrder;
import com.invman.common.entity.ReplenishmentOrderLine;
import com.invman.common.enums.ReplenishmentOrderStatus;
import com.invman.common.repository.InventoryItemRepository;
import com.invman.common.repository.ReplenishmentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplenishmentService {

    private final ReplenishmentOrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Transactional
    public ReplenishmentOrderStatusDto createOrder(ReplenishmentOrderRequest request) {
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
        return toDto(saved);
    }

    public ReplenishmentOrderStatusDto getOrder(Long id) {
        ReplenishmentOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        return toDto(order);
    }

    private ReplenishmentOrderStatusDto toDto(ReplenishmentOrder order) {
        return new ReplenishmentOrderStatusDto(
                order.getId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getSubmittedAt()
        );
    }
}

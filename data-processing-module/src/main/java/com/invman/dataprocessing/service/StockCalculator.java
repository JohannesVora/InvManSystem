package com.invman.dataprocessing.service;

import com.invman.common.entity.InventoryItem;
import com.invman.common.entity.InventoryTransaction;
import com.invman.common.repository.InventoryItemRepository;
import com.invman.common.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCalculator {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository transactionRepository;

    @Transactional
    public double recalculate(Long inventoryItemId) {
        InventoryItem item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException("InventoryItem not found: " + inventoryItemId));

        List<InventoryTransaction> transactions = transactionRepository.findByInventoryItemId(inventoryItemId);
        double total = transactions.stream()
                .mapToDouble(InventoryTransaction::getDelta)
                .sum();

        item.setCachedStock(total);
        item.setLastRecalculatedAt(LocalDateTime.now());
        inventoryItemRepository.save(item);

        return total;
    }
}

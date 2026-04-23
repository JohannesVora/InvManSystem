package com.invman.dataprocessing.service;

import com.invman.common.entity.InventoryItem;
import com.invman.common.entity.InventoryTransaction;
import com.invman.common.enums.TransactionType;
import com.invman.common.repository.InventoryItemRepository;
import com.invman.common.repository.InventoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockCalculatorTest {

    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock InventoryTransactionRepository transactionRepository;
    @InjectMocks StockCalculator stockCalculator;

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = new InventoryItem();
        item.setId(1L);
        item.setName("Flour");
        item.setCachedStock(0.0);
    }

    @Test
    void recalculatesStockFromTransactions() {
        InventoryTransaction t1 = new InventoryTransaction();
        t1.setInventoryItem(item);
        t1.setTransactionType(TransactionType.GOODS_RECEIPT);
        t1.setDelta(100.0);

        InventoryTransaction t2 = new InventoryTransaction();
        t2.setInventoryItem(item);
        t2.setTransactionType(TransactionType.POS_SALE);
        t2.setDelta(-30.0);

        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(transactionRepository.findByInventoryItemId(1L)).thenReturn(List.of(t1, t2));
        when(inventoryItemRepository.save(any())).thenReturn(item);

        double result = stockCalculator.recalculate(1L);

        assertThat(result).isEqualTo(70.0);
        verify(inventoryItemRepository).save(item);
    }

    @Test
    void returnsZeroWhenNoTransactions() {
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(transactionRepository.findByInventoryItemId(1L)).thenReturn(List.of());
        when(inventoryItemRepository.save(any())).thenReturn(item);

        double result = stockCalculator.recalculate(1L);

        assertThat(result).isEqualTo(0.0);
    }
}

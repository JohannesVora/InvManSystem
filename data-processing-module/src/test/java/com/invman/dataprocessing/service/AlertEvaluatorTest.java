package com.invman.dataprocessing.service;

import com.invman.common.entity.InventoryItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertEvaluatorTest {

    private final AlertEvaluator evaluator = new AlertEvaluator();

    @Test
    void needsReorderWhenStockAtOrBelowMinLevel() {
        InventoryItem item = new InventoryItem();
        item.setCachedStock(5.0);
        item.setMinStockLevel(10.0);

        assertThat(evaluator.needsReorder(item)).isTrue();
    }

    @Test
    void needsReorderWhenStockExactlyAtMinLevel() {
        InventoryItem item = new InventoryItem();
        item.setCachedStock(10.0);
        item.setMinStockLevel(10.0);

        assertThat(evaluator.needsReorder(item)).isTrue();
    }

    @Test
    void doesNotNeedReorderWhenStockAboveMinLevel() {
        InventoryItem item = new InventoryItem();
        item.setCachedStock(15.0);
        item.setMinStockLevel(10.0);

        assertThat(evaluator.needsReorder(item)).isFalse();
    }

    @Test
    void doesNotNeedReorderWhenStockIsNull() {
        InventoryItem item = new InventoryItem();
        item.setCachedStock(null);
        item.setMinStockLevel(10.0);

        assertThat(evaluator.needsReorder(item)).isFalse();
    }

    @Test
    void doesNotNeedReorderWhenMinStockLevelIsNull() {
        InventoryItem item = new InventoryItem();
        item.setCachedStock(5.0);
        item.setMinStockLevel(null);

        assertThat(evaluator.needsReorder(item)).isFalse();
    }
}

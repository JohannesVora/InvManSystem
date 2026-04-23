package com.invman.dataprocessing.service;

import com.invman.common.entity.InventoryItem;
import org.springframework.stereotype.Service;

@Service
public class AlertEvaluator {

    public boolean needsReorder(InventoryItem item) {
        if (item.getCachedStock() == null || item.getMinStockLevel() == null) {
            return false;
        }
        return item.getCachedStock() <= item.getMinStockLevel();
    }
}

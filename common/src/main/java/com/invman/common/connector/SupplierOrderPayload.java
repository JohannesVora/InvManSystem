package com.invman.common.connector;

import java.util.List;

public record SupplierOrderPayload(
        Long supplierOrderId,
        String supplierName,
        String orderDate,
        List<OrderLineItem> lines
) {
    public record OrderLineItem(
            String inventoryItemName,
            String supplierSku,
            Double orderedQty,
            String unit
    ) {}
}

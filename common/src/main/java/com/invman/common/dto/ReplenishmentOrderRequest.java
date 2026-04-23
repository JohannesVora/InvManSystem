package com.invman.common.dto;

import java.util.List;

public record ReplenishmentOrderRequest(List<OrderLine> lines) {

    public record OrderLine(Long inventoryItemId, Double requestedQty) {}
}

package com.invman.common.dto;

public record ReplenishmentOrderLineDto(
        Long id,
        Long inventoryItemId,
        String inventoryItemName,
        String inventoryItemUnit,
        Double requestedQty,
        Double receivedQty,
        String supplierSku,
        String packageUnit,
        Double conversionFactor) {}

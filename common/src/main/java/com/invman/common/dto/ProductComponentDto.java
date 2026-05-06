package com.invman.common.dto;

public record ProductComponentDto(
        Long id,
        Long salesProductId,
        String salesProductName,
        Long inventoryItemId,
        String inventoryItemName,
        String inventoryItemUnit,
        Double qtyRequired
) {}

package com.invman.common.dto;

public record CreateInventoryItemDto(
        String name,
        String unit,
        Double minStockLevel,   // nullable → default 0.0 in service
        Double reorderTarget    // nullable → default 0.0 in service
) {}

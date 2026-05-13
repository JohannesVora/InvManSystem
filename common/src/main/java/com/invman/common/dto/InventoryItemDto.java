package com.invman.common.dto;

public record InventoryItemDto(
        Long id,
        String name,
        String unit,
        Double cachedStock,
        Double minStockLevel,
        Double reorderTarget,
        boolean needsReorder,
        boolean hasPreferredOffer
) {}

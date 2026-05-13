package com.invman.common.dto;

public record UpdateInventoryItemDto(
        String name,
        String unit,
        Double minStockLevel,
        Double reorderTarget) {}

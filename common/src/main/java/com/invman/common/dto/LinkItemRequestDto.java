package com.invman.common.dto;

public record LinkItemRequestDto(
        Long inventoryItemId,
        Boolean isPreferred
) {}

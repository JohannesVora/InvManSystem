package com.invman.common.dto;

public record SupplierOfferDto(
        Long id,
        Long supplierId,
        String supplierSku,
        String supplierProductName,
        String packageUnit,
        Double unitPrice,
        Double conversionFactor,
        Boolean isPreferred,
        Long inventoryItemId,
        String inventoryItemName,
        String inventoryItemUnit
) {}

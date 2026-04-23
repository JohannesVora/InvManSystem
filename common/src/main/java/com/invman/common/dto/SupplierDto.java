package com.invman.common.dto;

public record SupplierDto(
        Long id,
        String name,
        String contactEmail,
        String phone
) {}

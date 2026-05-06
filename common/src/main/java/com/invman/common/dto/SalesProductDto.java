package com.invman.common.dto;

public record SalesProductDto(
        Long id,
        String externalId,
        String name,
        String posSystem
) {}

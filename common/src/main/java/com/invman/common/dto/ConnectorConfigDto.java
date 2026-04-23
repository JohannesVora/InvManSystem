package com.invman.common.dto;

public record ConnectorConfigDto(
        Long id,
        Long supplierId,
        String connectorTypeName,
        String configPayload,
        Boolean isActive
) {}

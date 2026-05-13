package com.invman.common.dto;

import com.invman.common.enums.ReplenishmentOrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ReplenishmentOrderDetailDto(
        Long id,
        ReplenishmentOrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        LocalDateTime receivedAt,
        List<ReplenishmentOrderLineDto> lines) {}

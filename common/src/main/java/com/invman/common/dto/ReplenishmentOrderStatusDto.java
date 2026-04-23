package com.invman.common.dto;

import com.invman.common.enums.ReplenishmentOrderStatus;

import java.time.LocalDateTime;

public record ReplenishmentOrderStatusDto(
        Long id,
        ReplenishmentOrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime submittedAt
) {}

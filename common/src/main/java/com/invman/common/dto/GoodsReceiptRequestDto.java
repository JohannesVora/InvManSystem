package com.invman.common.dto;

import java.util.List;

public record GoodsReceiptRequestDto(List<GoodsReceiptLineDto> lines) {}

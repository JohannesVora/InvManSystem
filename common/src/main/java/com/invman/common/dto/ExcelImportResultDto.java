package com.invman.common.dto;

import java.util.List;

public record ExcelImportResultDto(
        int imported,
        int updated,
        int skipped,
        List<String> skippedReasons
) {}

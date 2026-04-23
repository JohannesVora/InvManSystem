package com.invman.common.dto;

public record SmtpSettingsDto(
        String host,
        Integer port,
        String username,
        String password,
        String from
) {}

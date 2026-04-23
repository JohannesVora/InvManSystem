package com.invman.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        return attribute != null ? attribute.format(FORMATTER) : null;
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return LocalDateTime.parse(dbData, FORMATTER);
        } catch (Exception e1) {
            try {
                // Fallback: try without milliseconds (e.g. seed data from Flyway)
                return LocalDateTime.parse(dbData,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) {
                // Fallback: epoch milliseconds (legacy data stored as long by Hibernate)
                return LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(Long.parseLong(dbData)),
                        ZoneId.systemDefault());
            }
        }
    }
}

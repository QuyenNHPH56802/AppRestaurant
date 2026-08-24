package com.restaurant.server.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converts {@link Instant} to/from ISO-8601 TEXT for SQLite.
 *
 * The xerial sqlite-jdbc driver expects timestamps in the format
 * {@code yyyy-MM-dd HH:mm:ss.SSS} by default, but our schema stores them
 * as ISO-8601 UTC strings (e.g. {@code 2026-08-23T17:19:43.698Z}) so they
 * remain portable and human-readable.
 */
@Converter(autoApply = true)
public class InstantTextConverter implements AttributeConverter<Instant, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public String convertToDatabaseColumn(Instant attribute) {
        if (attribute == null) return null;
        return FORMATTER.format(attribute);
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        return Instant.parse(dbData);
    }
}
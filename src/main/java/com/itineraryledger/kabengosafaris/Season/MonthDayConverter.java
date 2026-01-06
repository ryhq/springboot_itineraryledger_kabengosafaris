package com.itineraryledger.kabengosafaris.Season;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

/**
 * JPA Converter for MonthDay to String
 *
 * Converts MonthDay objects to/from VARCHAR database columns
 * Format: "MM-dd" (e.g., "06-15" for June 15th, "12-25" for December 25th)
 *
 * This allows storing month-day combinations without year information,
 * perfect for recurring seasonal date ranges.
 */
@Converter(autoApply = true)
public class MonthDayConverter implements AttributeConverter<MonthDay, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public String convertToDatabaseColumn(MonthDay monthDay) {
        return monthDay != null ? monthDay.format(FORMATTER) : null;
    }

    @Override
    public MonthDay convertToEntityAttribute(String dbData) {
        return dbData != null && !dbData.isEmpty() ? MonthDay.parse(dbData, FORMATTER) : null;
    }
}

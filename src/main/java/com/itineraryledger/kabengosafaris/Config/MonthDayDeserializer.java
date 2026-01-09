package com.itineraryledger.kabengosafaris.Config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

/**
 * Jackson JSON Deserializer for MonthDay
 *
 * Deserializes JSON strings in "MM-DD" format to MonthDay objects
 * Example: "12-15" -> MonthDay.of(12, 15)
 */
public class MonthDayDeserializer extends JsonDeserializer<MonthDay> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public MonthDay deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isEmpty()) {
            return null;
        }
        return MonthDay.parse(value, FORMATTER);
    }
}

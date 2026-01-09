package com.itineraryledger.kabengosafaris.Config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

/**
 * Jackson JSON Serializer for MonthDay
 *
 * Serializes MonthDay objects to JSON strings in "MM-DD" format
 * Example: MonthDay.of(12, 15) -> "12-15"
 */
public class MonthDaySerializer extends JsonSerializer<MonthDay> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public void serialize(MonthDay value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
            gen.writeString(value.format(FORMATTER));
        }
    }
}

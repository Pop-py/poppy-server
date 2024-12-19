package com.poppy.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

// 날짜 형식 지정 클래스 (yyyy.MM.dd (요일))
public class LocalDateWithDayOfWeekSerializer extends JsonSerializer<LocalDate> {
    @Override
    public void serialize(LocalDate date, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN);
        String formattedDate = String.format("%s(%s)",
                date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd ")),
                dayOfWeek);
        gen.writeString(formattedDate);
    }
}

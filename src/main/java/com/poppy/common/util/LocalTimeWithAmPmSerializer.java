package com.poppy.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalTime;

// 시간 형식 지정 클래스 (HH:mm)
public class LocalTimeWithAmPmSerializer extends JsonSerializer<LocalTime> {
    @Override
    public void serialize(LocalTime time, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String amPm = time.getHour() < 12 ? "오전" : "오후";

        // 12시간제로 변환
        int hour = time.getHour() % 12;
        if (hour == 0) hour = 12;  // 0시는 12시로 표시

        String formattedTime = String.format("%s %02d:%02d",
                amPm,
                hour,
                time.getMinute());

        gen.writeString(formattedTime);
    }
}

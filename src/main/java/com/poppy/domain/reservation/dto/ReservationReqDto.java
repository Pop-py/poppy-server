package com.poppy.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class ReservationReqDto {
    @NotNull(message = "팝업 스토어를 입력해주세요.")
    Long popupStoreId;

    @NotNull(message = "날짜를 입력해주세요.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate date;

    @NotNull(message = "시간을 입력해주세요.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    LocalTime time;

    @NotNull(message = "인원을 입력해주세요.")
    Integer person;
}

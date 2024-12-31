package com.poppy.domain.reservation.controller;

import com.poppy.domain.reservation.service.ReservationReminderScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ReservationTestRestController {
    private final ReservationReminderScheduler reminderScheduler;

    @PostMapping("/trigger-reminder")
    public ResponseEntity<String> triggerReminder() {
        reminderScheduler.sendReservationReminders();
        return ResponseEntity.ok("Reminder scheduler triggered");
    }
}
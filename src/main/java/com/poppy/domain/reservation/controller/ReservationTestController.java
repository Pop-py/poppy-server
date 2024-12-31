package com.poppy.domain.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
public class ReservationTestController {

    @GetMapping("/notification")
    public String showNotificationPage(Model model) {
        return "notification-test";
    }
}
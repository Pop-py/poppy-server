package com.poppy.domain.reservation.controller;


import com.poppy.domain.popupStore.service.PopupStoreService;
import com.poppy.domain.popupStore.dto.response.ReservationAvailableSlotDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class TestController {


    private final PopupStoreService popupStoreService;

    @PostMapping("/{storeId}/initialize")
    public ResponseEntity<String> initializeSlots(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "10") int defaultSlot) {
        popupStoreService.initializeSlots(storeId, defaultSlot);
        return ResponseEntity.ok("Slots initialized for storeId: " + storeId);
    }

    @GetMapping("/{storeId}/{date}")
    public ResponseEntity<List<ReservationAvailableSlotDTO>> getAvailable(@PathVariable Long storeId, @PathVariable LocalDate date){
        List<ReservationAvailableSlotDTO> availables = popupStoreService.getAvailableSlots(storeId,date);
        return ResponseEntity.ok(availables);
    }
}

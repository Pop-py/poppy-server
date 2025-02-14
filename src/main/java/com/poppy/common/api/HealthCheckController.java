package com.poppy.common.api;

import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthCheckController {
    private final PopupStoreRepository popupStoreRepository;

    @GetMapping("/")
    public ResponseEntity<String> healthCheck() {
        try {
            String storeName = popupStoreRepository.findById(1L)
                    .map(PopupStore::getName)
                    .orElse("Store not found");

            return new ResponseEntity<>("OK - DB Connected: " + storeName, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("DB Connection Failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

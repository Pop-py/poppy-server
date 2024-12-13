package com.poppy.domain.waiting.repository;

import com.poppy.domain.waiting.entity.WaitingSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaitingSettingsRepository extends JpaRepository<WaitingSettings, Long> {
    Optional<WaitingSettings> findByPopupStoreId(Long popupStoreId);
}
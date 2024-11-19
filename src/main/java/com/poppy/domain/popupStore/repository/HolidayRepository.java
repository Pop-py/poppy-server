package com.poppy.domain.popupStore.repository;

import com.poppy.domain.popupStore.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday,Long> {

    List<Holiday> findByPopupStoreId(Long id);
}

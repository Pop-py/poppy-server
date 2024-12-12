package com.poppy.admin.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.dto.request.PopupStoreReqDto;
import com.poppy.domain.popupStore.dto.response.PopupStoreRspDto;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.ReservationType;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.popupStore.service.PopupStoreService;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import com.poppy.domain.storeCategory.repository.StoreCategoryRepository;
import com.poppy.domain.storeCategory.entity.StoreCategory;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final PopupStoreRepository popupStoreRepository;
    private final StoreCategoryRepository storeCategoryRepository;
    private final UserRepository userRepository;
    private final ReservationAvailableSlotRepository reservationAvailableSlotRepository;
    private final PopupStoreService popupStoreService;
    private final AsyncRedisSlotInitializationService asyncRedisSlotService;

    @Transactional
    public PopupStoreRspDto savePopupStore(PopupStoreReqDto reqDto) {
        // 카테고리 존재 여부 확인
        StoreCategory category = storeCategoryRepository.findByName(reqDto.getCategoryName())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 관리자 유저 존재 여부 확인
        User masterUser = userRepository.findById(reqDto.getMasterUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        masterUser.upgradeToMaster();

        // 날짜/시간 유효성 검증
        validateDateAndTime(reqDto);

        // PopupStore 엔티티 생성 및 저장
        PopupStore popupStore = PopupStore.builder()
                .name(reqDto.getName())
                .location(reqDto.getLocation())
                .address(reqDto.getAddress())
                .startDate(reqDto.getStartDate())
                .endDate(reqDto.getEndDate())
                .openingTime(reqDto.getOpeningTime())
                .closingTime(reqDto.getClosingTime())
                .availableSlot(reqDto.getAvailableSlot())
                .storeCategory(category)
                .isActive(true)
                .isEnd(false)
                .rating(0.0)
                .price(reqDto.getPrice())
                .masterUser(masterUser)
                .reservationType(reqDto.getReservationType())
                .thumbnail(reqDto.getThumbnail())
                .build();

        PopupStore savedPopupStore = popupStoreRepository.save(popupStore);

        // 휴무일 설정
        if(reqDto.getHolidays() != null && !reqDto.getHolidays().isEmpty()) {
            List<ReservationAvailableSlot> holidaySlots = reqDto.getHolidays().stream()
                    .flatMap(date -> {
                        List<ReservationAvailableSlot> slotsForDay = new ArrayList<>();
                        LocalTime currentTime = reqDto.getOpeningTime();

                        // 영업 시작시간부터 종료시간 직전까지 1시간 단위로 슬롯 생성
                        while (currentTime.isBefore(reqDto.getClosingTime())) {
                            slotsForDay.add(ReservationAvailableSlot.builder()
                                    .popupStore(savedPopupStore)
                                    .date(date)
                                    .time(currentTime)
                                    .availableSlot(0)
                                    .totalSlot(0)
                                    .status(PopupStoreStatus.HOLIDAY)
                                    .build());

                            currentTime = currentTime.plusHours(1);
                        }
                        return slotsForDay.stream();
                    })
                    .toList();

            reservationAvailableSlotRepository.saveAll(holidaySlots);
        }

        // 예약 슬롯 초기화 (이미 휴무일로 설정된 날짜는 제외됨)
        if(savedPopupStore.getReservationType() == ReservationType.ONLINE) {
            popupStoreService.initializeSlots(savedPopupStore.getId());
            asyncRedisSlotService.initializeRedisSlots(savedPopupStore.getId());
        }

        return PopupStoreRspDto.from(savedPopupStore);
    }

    private void validateDateAndTime(PopupStoreReqDto reqDto) {
        LocalDate currentDate = LocalDate.now();

        try {
            // 시작일과 종료일 검증
            LocalDate startDate = reqDto.getStartDate();
            LocalDate endDate = reqDto.getEndDate();

            if (startDate.isBefore(currentDate))
                throw new BusinessException(ErrorCode.INVALID_START_DATE);

            if (endDate.isBefore(startDate))
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);

            // 오픈 시간과 종료 시간 검증
            if (reqDto.getClosingTime().isBefore(reqDto.getOpeningTime()))
                throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
        }
        catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_DATE);
        }
    }

    // 팝업 스토어 삭제
    @Transactional
    public void deletePopupStore(Long id) {
        PopupStore popupStore = popupStoreRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        try {
            asyncRedisSlotService.clearRedisData(popupStore.getId());
            popupStoreRepository.delete(popupStore);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.STORE_HAS_REFERENCES);
        }
    }
}
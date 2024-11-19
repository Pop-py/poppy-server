package com.poppy.domain.popupStore.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.dto.response.PopupStoreCalenderDTO;
import com.poppy.domain.popupStore.dto.response.PopupStoreResponseDto;
import com.poppy.domain.popupStore.entity.Holiday;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.HolidayRepository;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.popupStore.dto.response.ReservationAvailableSlotDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopupStoreService {
    private final PopupStoreRepository popupStoreRepository;
    private final HolidayRepository holidayRepository;
    private final RedisTemplate<String, Integer> redisTemplate;


    // 전체 목록 조회
    @Transactional(readOnly = true)
    public List<PopupStoreResponseDto> getAllActiveStores() {
        List<PopupStore> stores = popupStoreRepository.findAllActive();
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 팝업 스토어 상세 조회
    @Transactional(readOnly = true)
    public PopupStoreResponseDto getPopupStore(Long id){
        PopupStore popupStore =  popupStoreRepository.findById(id).orElseThrow(()->new BusinessException(ErrorCode.STORE_NOT_FOUND));

        return convertToDto(popupStore);
    }

    // 카테고리별 조회
    @Transactional(readOnly = true)
    public List<PopupStoreResponseDto> getStoresByCategory(Long categoryId) {
        List<PopupStore> stores = popupStoreRepository.findByCategoryId(categoryId);
        if (stores.isEmpty()) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 위치별 조회
    @Transactional(readOnly = true)
    public List<PopupStoreResponseDto> getStoresByLocation(String location) {
        List<PopupStore> stores = popupStoreRepository.findByLocation(location);
        if (stores.isEmpty()) {
            throw new BusinessException(ErrorCode.LOCATION_NOT_FOUND);
        }
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 날짜별 조회
    @Transactional(readOnly = true)
    public List<PopupStoreResponseDto> getStoresByDate(LocalDate startDate, LocalDate endDate) {
        // 시작일/종료일이 null이면 오늘 날짜로 설정
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();

        // 시작일이 종료일보다 늦은 경우 예외 발생
        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        List<PopupStore> stores = popupStoreRepository.findByDateRange(effectiveStartDate, effectiveEndDate);
        if (stores.isEmpty()) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 이름으로 검색
    @Transactional(readOnly = true)
    public List<PopupStoreResponseDto> searchStoresByName(String name) {
        List<PopupStore> stores = popupStoreRepository.findByKeyword(name);
        if (stores.isEmpty()) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 신규 스토어 조회 (일주일 이내)
    @Transactional(readOnly = true)
    public List<PopupStoreResponseDto> getNewStores() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<PopupStore> stores = popupStoreRepository.findNewStores(weekAgo);
        if (stores.isEmpty()) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    //팝업스토어 캘린더 반환(오픈 기간)
    @Transactional(readOnly = true)
    public PopupStoreCalenderDTO getCalender(Long id){
        PopupStore popupStore = popupStoreRepository.findById(id).orElseThrow(()->new BusinessException(ErrorCode.STORE_NOT_FOUND));

        List<LocalDate> holidays = holidayRepository.findByPopupStoreId(id) //해당 팝업 스토어의 휴무일 조회
                .stream()
                .map(Holiday::getHolidayDate)
                .filter(localDate -> !localDate.isBefore(LocalDate.now())) //오늘 날짜 이전 휴무일은 가져오지 않음
                .collect(Collectors.toList());

        LocalDate startDate = popupStore.getStartDate().isBefore(LocalDate.now()) // 오늘 이전 날짜는 예약 가능일 설정 x
                ? LocalDate.now()
                : popupStore.getStartDate();

        return PopupStoreCalenderDTO.builder() // DTO 변환 후 반환
                .id(popupStore.getId())
                .name(popupStore.getName())
                .startDate(startDate)
                .endDate(popupStore.getEndDate())
                .holidays(holidays)
                .build();
    }

    // 팝업스토어 생성시 redis 에 슬롯 초기화
    // slot은 일단 모두 생성
    // Holdiay,현재보다 과거 시간대(지나가 버린 시간) 에 대한 처리는 GetAvailableSlots API 에서 처리


    public void initializeSlots(Long storeId, int defaultSlot) {
        // 팝업스토어 조회
        PopupStore popupStore = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 운영 기간 설정
        List<LocalDateTime> slotDateTimeList = new ArrayList<>();
        LocalDate currentDate = popupStore.getStartDate();
        LocalDate endDate = popupStore.getEndDate();

        // 1. 모든 슬롯 시간을 리스트에 추가
        while (!currentDate.isAfter(endDate)) {
            LocalTime currentTime = popupStore.getOpeningTime();
            LocalTime closingTime = popupStore.getClosingTime();

            while (currentTime.isBefore(closingTime)) {
                slotDateTimeList.add(LocalDateTime.of(currentDate, currentTime));
                currentTime = currentTime.plusHours(1); // 1시간 단위
            }
            currentDate = currentDate.plusDays(1);
        }

        // 2. 슬롯 리스트를 가까운 날짜, 시간 순으로 정렬
        slotDateTimeList.sort(Comparator.naturalOrder());

        // 3. 정렬된 슬롯 리스트를 Redis에 저장
        LocalDateTime now = LocalDateTime.now();
        for (LocalDateTime slotDateTime : slotDateTimeList) {
            if (slotDateTime.isBefore(now)) {
                continue; // 이미 지난 시간은 저장하지 않음
            }

            String redisKey = String.format("reservation:%d:%s:%s",
                    storeId,
                    slotDateTime.toLocalDate(),
                    slotDateTime.toLocalTime()
            );

            // Redis에 슬롯 데이터 저장
            redisTemplate.opsForValue().set(redisKey, defaultSlot);

            // TTL 설정: 자정 만료
            long ttl = Duration.between(now, slotDateTime.toLocalDate().atStartOfDay().plusDays(1)).toSeconds();
            redisTemplate.expire(redisKey, ttl, TimeUnit.SECONDS);
        }
    }

    @Transactional(readOnly = true)
    public List<ReservationAvailableSlotDTO> getAvailableSlots(Long storeId, LocalDate date) { // 특정 Store 의 특정 날짜의 예약 가능 시간대 조회
        // 팝업스토어 조회
        PopupStore popupStore = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 팝업스토어 운영 시간 가져오기
        LocalTime openingTime = popupStore.getOpeningTime();
        LocalTime closingTime = popupStore.getClosingTime();

        List<ReservationAvailableSlotDTO> availableSlots = new ArrayList<>();

        LocalTime currentTime = openingTime;
        LocalDateTime now = LocalDateTime.now();

        while (currentTime.isBefore(closingTime)) {
            // 현재 날짜와 시간 기준으로 이미 지나간 시간 필터링
            LocalDateTime slotDateTime = LocalDateTime.of(date, currentTime);
            if (slotDateTime.isBefore(now)) {
                currentTime = currentTime.plusHours(1); // 다음 시간대로 이동
                continue; // 지나간 시간은 건너뜀
            }

            // Redis에서 슬롯 데이터 조회
            String redisKey = String.format("reservation:%d:%s:%s", storeId, date, currentTime);
            Integer availableSlot = redisTemplate.opsForValue().get(redisKey);

            // DTO 생성 및 추가
            availableSlots.add(
                    ReservationAvailableSlotDTO.builder()
                            .time(currentTime.toString())
                            .availableSlot(availableSlot != null ? availableSlot : 0)
                            .isAvailable(availableSlot != null && availableSlot > 0)
                            .build()
            );

            currentTime = currentTime.plusHours(1);
        }

        return availableSlots;
    }

    // Entity -> DTO 변환
    private PopupStoreResponseDto convertToDto(PopupStore store) {
        return PopupStoreResponseDto.builder()
                .id(store.getId())
                .name(store.getName())
                .location(store.getLocation())
                .address(store.getAddress())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .startDate(store.getStartDate())
                .endDate(store.getEndDate())
                .openingTime(store.getOpeningTime())    // 운영 시작 시간 추가
                .closingTime(store.getClosingTime())    // 운영 종료 시간 추가
                .availableSlot(store.getAvailableSlot())
                .isActive(store.getIsActive())
                .isEnd(store.getIsEnd())                // 종료 여부 추가
                .rating(store.getRating())
                .categoryName(store.getStoreCategory() != null
                        ? store.getStoreCategory().getName()
                        : "Unknown")                        // 카테고리 이름 (Null 체크 추가)
                .thumbnail(store.getThumbnail())        // 썸네일 경로 추가
                .build();
    }



}
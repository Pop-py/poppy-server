package com.poppy.domain.popupStore.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.dto.response.PopupStoreResponseDto;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopupStoreService {
    private final PopupStoreRepository popupStoreRepository;

    // 전체 목록 조회
    public List<PopupStoreResponseDto> getAllActiveStores() {
        List<PopupStore> stores = popupStoreRepository.findAllActive();
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 팝업 스토어 상세 조회
    public PopupStoreResponseDto getPopupStore(Long id){
        PopupStore popupStore =  popupStoreRepository.findById(id).orElseThrow(()->new BusinessException(ErrorCode.STORE_NOT_FOUND));

        return convertToDto(popupStore);
    }


    // 카테고리별 조회
    public List<PopupStoreResponseDto> getStoresByCategory(Long categoryId) {
        List<PopupStore> stores = popupStoreRepository.findByCategoryId(categoryId);
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 위치별 조회
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
    public List<PopupStoreResponseDto> getStoresByDate(LocalDate startDate, LocalDate endDate) {
        List<PopupStore> stores = popupStoreRepository.findByDateRange(startDate, endDate);
        if (stores.isEmpty()) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return stores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 이름으로 검색
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
                .time(store.getTime())
                .availableSlot(store.getAvailableSlot())
                .isActive(store.getIsActive())
                .rating(store.getRating())
                .categoryName(store.getStoreCategory().getName())
                .build();
    }
}
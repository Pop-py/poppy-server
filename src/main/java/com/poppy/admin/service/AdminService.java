package com.poppy.admin.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.dto.request.PopupStoreReqDto;
import com.poppy.domain.popupStore.dto.response.PopupStoreRspDto;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.ReservationType;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.popupStore.service.PopupStoreService;
import com.poppy.domain.storeCategory.repository.StoreCategoryRepository;
import com.poppy.domain.storeCategory.entity.StoreCategory;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final PopupStoreRepository popupStoreRepository;
    private final StoreCategoryRepository storeCategoryRepository;
    private final UserRepository userRepository;
    private final PopupStoreService popupStoreService;

    public PopupStoreRspDto savePopupStore(PopupStoreReqDto reqDto) { // 팝업스토어 등록 + (예약일 경우)slot initialize
        // 1. 카테고리 존재 여부 확인
        StoreCategory category = storeCategoryRepository.findByName(reqDto.getCategoryName())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. 관리자 유저 존재 여부 확인
        User masterUser = userRepository.findById(reqDto.getMasterUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        masterUser.upgradeToMaster();

        // 3. 날짜/시간 유효성 검증
        validateDateAndTime(reqDto);

        // 4. PopupStore 엔티티 생성 및 저장
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
                .masterUser(masterUser)
                .reservationType(reqDto.getReservationType())
                .thumbnail(reqDto.getThumbnail())
                .build();

        PopupStore savedPopupStore = popupStoreRepository.save(popupStore);

        if(savedPopupStore.getReservationType() == ReservationType.ONLINE){ // 예약이 가능한 팝업스토어의 경우에만
            popupStoreService.initializeSlots(savedPopupStore.getId());
        }


        // 5. 응답 DTO 변환 및 반환
        return PopupStoreRspDto.from(savedPopupStore);
    }


    private void validateDateAndTime(PopupStoreReqDto reqDto) {
        LocalDate currentDate = LocalDate.now();

        if (reqDto.getStartDate().isBefore(currentDate)) {
            throw new IllegalArgumentException("시작일은 현재 날짜보다 이전일 수 없습니다.");
        }

        if (reqDto.getEndDate().isBefore(reqDto.getStartDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 이전일 수 없습니다.");
        }

        if (reqDto.getClosingTime().isBefore(reqDto.getOpeningTime())) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 이전일 수 없습니다.");
        }
    }

    public void deletePopupStore(Long id) { //팝업 스토어 삭제

        PopupStore popupStore = popupStoreRepository.findById(id)
                .orElseThrow(()->new BusinessException(ErrorCode.STORE_NOT_FOUND));

        popupStoreRepository.delete(popupStore);
    }


}
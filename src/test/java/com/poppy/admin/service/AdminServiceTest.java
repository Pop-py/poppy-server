package com.poppy.admin.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.popupStore.dto.request.PopupStoreReqDto;
import com.poppy.domain.popupStore.dto.response.PopupStoreRspDto;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.ReservationType;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.popupStore.service.PopupStoreService;
import com.poppy.domain.storeCategory.entity.StoreCategory;
import com.poppy.domain.storeCategory.repository.StoreCategoryRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock
    private PopupStoreRepository popupStoreRepository;
    @Mock
    private StoreCategoryRepository storeCategoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AsyncRedisSlotInitializationService asyncRedisSlotService;
    @Mock
    private PopupStoreService popupStoreService;

    @InjectMocks
    private AdminService adminService;

    private PopupStoreReqDto reqDto;
    private PopupStore popupStore;
    private User masterUser;
    private StoreCategory category;

    @BeforeEach
    void setUp() {
        masterUser = User.builder()
                .id(1L)
                .build();

        category = StoreCategory.builder()
                .id(1L)
                .name("카테고리")
                .build();

        reqDto = PopupStoreReqDto.builder()
                .name("테스트 스토어")
                .location("테스트 위치")
                .address("테스트 주소")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(7))
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .availableSlot(100)
                .categoryName("카테고리")
                .masterUserId(1L)
                .reservationType(ReservationType.ONLINE)
                .build();

        popupStore = PopupStore.builder()
                .id(1L)
                .name("테스트 스토어")
                .location("테스트 위치")
                .address("테스트 주소")
                .storeCategory(category)
                .masterUser(masterUser)
                .reservationType(ReservationType.ONLINE)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(7))
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .availableSlot(100)
                .isActive(true)
                .isEnd(false)
                .rating(0.0)
                .build();
    }

    @Test
    void 관리자_스토어_등록() {
        // given
        when(storeCategoryRepository.findByName(anyString()))
                .thenReturn(Optional.of(category));
        when(userRepository.findById(anyLong()))
                .thenReturn(Optional.of(masterUser));
        when(popupStoreRepository.save(any(PopupStore.class)))
                .thenReturn(popupStore);
        doNothing().when(popupStoreService).initializeSlots(anyLong());

        // when
        PopupStoreRspDto result = adminService.savePopupStore(reqDto);

        // then
        assertNotNull(result);
        assertEquals("테스트 스토어", result.getName());
        verify(popupStoreRepository).save(any(PopupStore.class));
        verify(popupStoreService).initializeSlots(anyLong());
        verify(asyncRedisSlotService).initializeRedisSlots(anyLong());
    }

    @Test
    void Redis_추가_예외_시_데이터_확인() {
        // given
        when(storeCategoryRepository.findByName(anyString()))
                .thenReturn(Optional.of(category));
        when(userRepository.findById(anyLong()))
                .thenReturn(Optional.of(masterUser));
        when(popupStoreRepository.save(any(PopupStore.class)))
                .thenReturn(popupStore);
        doNothing().when(popupStoreService).initializeSlots(anyLong());

        // Redis 에러를 던지되 비동기 작업이므로 void를 반환
        doAnswer(invocation -> null)
                .when(asyncRedisSlotService)
                .initializeRedisSlots(anyLong());

        // when
        PopupStoreRspDto result = adminService.savePopupStore(reqDto);

        // then
        assertNotNull(result);
        assertEquals("테스트 스토어", result.getName());
        verify(popupStoreRepository).save(any(PopupStore.class));
        verify(asyncRedisSlotService).initializeRedisSlots(anyLong());
    }

    @Test
    void 관리자_스토어_삭제() {
        // given
        when(popupStoreRepository.findById(anyLong()))
                .thenReturn(Optional.of(popupStore));

        // when
        adminService.deletePopupStore(1L);

        // then
        verify(popupStoreRepository).delete(any(PopupStore.class));
        verify(asyncRedisSlotService).clearRedisData(anyLong());
    }

    @Test
    void Redis_삭제_예외_시_데이터_확인() {
        // given
        when(popupStoreRepository.findById(anyLong()))
                .thenReturn(Optional.of(popupStore));
        doThrow(new DataIntegrityViolationException("참조 무결성 위반"))
                .when(popupStoreRepository)
                .delete(any(PopupStore.class));

        // when & then
        assertThrows(BusinessException.class, () -> adminService.deletePopupStore(1L));
        verify(asyncRedisSlotService).clearRedisData(anyLong());
        verify(popupStoreRepository).delete(any(PopupStore.class));
    }
}
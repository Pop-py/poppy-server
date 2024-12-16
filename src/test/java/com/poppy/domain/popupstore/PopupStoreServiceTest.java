package com.poppy.domain.popupstore;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.popupStore.dto.response.PopupStoreRspDto;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.ReservationType;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.popupStore.repository.PopupStoreViewRepository;
import com.poppy.domain.popupStore.service.PopupStoreService;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import com.poppy.domain.storeCategory.entity.StoreCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopupStoreServiceTest {

    @Mock
    private PopupStoreRepository popupStoreRepository;

    @Mock
    private PopupStoreViewRepository popupStoreViewRepository;

    @Mock
    private ReservationAvailableSlotRepository reservationAvailableSlotRepository;

    @InjectMocks
    private PopupStoreService popupStoreService;

    private PopupStore store1;
    private PopupStore store2;
    private StoreCategory category;

    @BeforeEach
    void setUp() {
        category = StoreCategory.builder()
                .id(1L)
                .name("패션")
                .build();

        store1 = PopupStore.builder()
                .id(1L)
                .name("테스트 팝업 1")
                .location("서울")
                .address("서울시 강남구")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .openingTime(LocalTime.of(10, 0))
                .closingTime(LocalTime.of(20, 0))
                .availableSlot(100)
                .isActive(true)
                .isEnd(false)
                .storeCategory(category)
                .reservationType(ReservationType.ONLINE)
                .build();

        store2 = PopupStore.builder()
                .id(2L)
                .name("테스트 팝업 2")
                .location("부산")
                .address("부산시 해운대구")
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(60))
                .openingTime(LocalTime.of(11, 0))
                .closingTime(LocalTime.of(21, 0))
                .availableSlot(150)
                .isActive(true)
                .isEnd(false)
                .storeCategory(category)
                .reservationType(ReservationType.ONLINE)
                .build();
    }

    @Nested
    @DisplayName("팝업스토어 목록 조회 테스트")
    class GetAllActiveStoresTest {
        @Test
        void 활성화된_모든_팝업스토어_조회_성공() {
            // given
            when(popupStoreRepository.findAllActive()).thenReturn(Arrays.asList(store1, store2));

            // when
            List<PopupStoreRspDto> result = popupStoreService.getAllActiveStores();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("테스트 팝업 1");
            assertThat(result.get(1).getName()).isEqualTo("테스트 팝업 2");
        }
    }

    @Nested
    @DisplayName("팝업스토어 상세 조회 테스트")
    class GetPopupStoreTest {
        @Test
        void 팝업스토어_상세_조회_성공() {
            // given
            when(popupStoreRepository.findById(1L)).thenReturn(Optional.of(store1));
            when(popupStoreViewRepository.save(any())).thenReturn(null);

            // when
            PopupStoreRspDto result = popupStoreService.getPopupStore(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("테스트 팝업 1");
            verify(popupStoreViewRepository, times(1)).save(any());
        }

        @Test
        void 존재하지_않는_팝업스토어_조회시_예외_발생() {
            // given
            when(popupStoreRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> popupStoreService.getPopupStore(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("해당 팝업스토어를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("인기 팝업스토어 조회 테스트")
    class GetPopularPopupStoresTest {
        @Test
        void 인기_팝업스토어_조회_성공() {
            // given
            Object[] result1 = new Object[]{1L, 100L};
            Object[] result2 = new Object[]{2L, 80L};
            List<Object[]> resultList = Arrays.asList(result1, result2);
            Page<Object[]> pageResult = new PageImpl<>(resultList);

            when(popupStoreViewRepository.findPopularPopupStores(any(), any())).thenReturn(pageResult);
            when(popupStoreRepository.findAllById(any())).thenReturn(Arrays.asList(store1, store2));

            // when
            List<PopupStoreRspDto> result = popupStoreService.getPopularPopupStores();

            // then
            assertThat(result).hasSize(2);
            verify(popupStoreViewRepository).findPopularPopupStores(any(), any());
        }
    }

    @Nested
    @DisplayName("날짜별 팝업스토어 조회 테스트")
    class GetStoresByDateTest {
        @Test
        void 날짜_범위로_팝업스토어_조회_성공() {
            // given
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            when(popupStoreRepository.findByDateRange(startDate, endDate))
                    .thenReturn(Arrays.asList(store1, store2));

            // when
            List<PopupStoreRspDto> result = popupStoreService.getStoresByDate(startDate, endDate);

            // then
            assertThat(result).hasSize(2);
            verify(popupStoreRepository).findByDateRange(startDate, endDate);
        }

        @Test
        void 잘못된_날짜_범위로_조회시_예외_발생() {
            // given
            LocalDate startDate = LocalDate.now().plusDays(7);
            LocalDate endDate = LocalDate.now();

            // when & then
            assertThatThrownBy(() -> popupStoreService.getStoresByDate(startDate, endDate))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("시작일이 종료일보다 늦을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("오픈 예정 팝업스토어 조회 테스트")
    class GetAllFuturePopupStoresTest {
        @Test
        void 오픈_예정_팝업스토어_조회_성공() {
            // given
            when(popupStoreRepository.findAllFuturePopupStores(any()))
                    .thenReturn(Arrays.asList(store1, store2));

            // when
            List<PopupStoreRspDto> result = popupStoreService.getAllFuturePopupStores();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("테스트 팝업 1");
            assertThat(result.get(1).getName()).isEqualTo("테스트 팝업 2");
            verify(popupStoreRepository).findAllFuturePopupStores(any());
        }
    }
}
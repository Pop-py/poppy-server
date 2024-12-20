package com.poppy.domain.popupStore.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.QPopupStore;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class PopupStoreRepositoryImpl implements PopupStoreRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QPopupStore store = QPopupStore.popupStore;

    // 전체 목록 조회 (활성화된 스토어만)
    @Override
    public List<PopupStore> findAllActive() {
        return queryFactory
                .selectFrom(store)
                // 모든 유형의 팝업 스토어를 가져오기 위해 left join 사용
                .leftJoin(store.reservationAvailableSlots).fetchJoin()
                .leftJoin(store.storeCategory).fetchJoin()
                .where(isEndFalse())
                .orderBy(store.createTime.desc())
                .distinct() // 중복 제거
                .fetch();
    }

    // 카테고리별 조회
    @Override
    public List<PopupStore> findByCategoryId(Long categoryId) {
        return queryFactory
                .selectFrom(store)
                .where(
                        isEndFalse(),
                        store.storeCategory.id.eq(categoryId)
                )
                .orderBy(store.createTime.desc())
                .fetch();
    }

    // 위치별 조회
    @Override
    public List<PopupStore> findByLocation(String location) {
        return queryFactory
                .selectFrom(store)
                .where(
                        isEndFalse(),
                        locationContains(location)
                )
                .orderBy(store.createTime.desc())
                .fetch();
    }

    // 날짜 범위로 조회
    @Override
    public List<PopupStore> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .selectFrom(store)
                .where(
                        isEndFalse(),
                        dateBetween(startDate, endDate)
                )
                .orderBy(store.createTime.desc())
                .fetch();
    }

    // 이름으로 검색
    @Override
    public List<PopupStore> findByKeyword(String name) {
        return queryFactory
                .selectFrom(store)
                .where(
                        isEndFalse(),
                        nameContains(name)
                )
                .orderBy(store.createTime.desc())
                .fetch();
    }

    // 신규 스토어 조회
    @Override
    public List<PopupStore> findNewStores(LocalDateTime fromDate) {
        return queryFactory
                .selectFrom(store)
                .where(
                        isEndFalse(),
                        createTimeAfter(fromDate)
                )
                .orderBy(store.createTime.desc())
                .fetch();
    }

    // 조건식들
    private BooleanExpression isEndFalse() {
        return store.isEnd.eq(false);
    }

    private BooleanExpression locationContains(String location) {
        return location != null ? store.location.contains(location) : null;
    }

    private BooleanExpression dateBetween(LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();

        return store.startDate.loe(effectiveEndDate)
                .and(store.endDate.goe(effectiveStartDate));
    }

    private BooleanExpression nameContains(String name) {
        return name != null ? store.name.contains(name) : null;
    }

    private BooleanExpression createTimeAfter(LocalDateTime fromDate) {
        return store.createTime.goe(fromDate);
    }
}
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
                .where(
                        isEndFalse()
                )
                .orderBy(store.baseTime.createTime.desc())
                .fetch();
    }

    // 카테고리별 조회
    @Override
    public List<PopupStore> findByCategory(String categoryName) {
        return queryFactory
                .selectFrom(store)
                .where(
                        isEndFalse(),
                        categoryEq(categoryName)
                )
                .orderBy(store.baseTime.createTime.desc())
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
                .orderBy(store.baseTime.createTime.desc())
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
                .orderBy(store.baseTime.createTime.desc())
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
                .orderBy(store.baseTime.createTime.desc())
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
                .orderBy(store.baseTime.createTime.desc())
                .fetch();
    }

    // 조건식들
    private BooleanExpression isEndFalse() {
        return store.isEnd.eq(false);
    }

    private BooleanExpression categoryEq(String categoryName) {
        return categoryName != null ? store.storeCategory.name.eq(categoryName) : null;
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
        return store.baseTime.createTime.goe(fromDate);
    }
}
package com.poppy.domain.popupStore.repository;

import com.poppy.domain.popupStore.dto.request.PopupStoreSearchReqDto;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.QPopupStore;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
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

    // 팝업 스토어 검색 필터링
    @Override
    public List<PopupStore> findBySearchCondition(PopupStoreSearchReqDto searchDto) {
        return queryFactory
                .selectFrom(store)
                .leftJoin(store.reservationAvailableSlots).fetchJoin()
                .leftJoin(store.storeCategory).fetchJoin()
                .where(
                        isEndFalse(),
                        dateEquals(searchDto.getDate()),
                        locationIn(searchDto.getLocations()),
                        ratingGoe(searchDto.getRating()),
                        categoryIn(searchDto.getCategoryIds())
                )
                .orderBy(store.createTime.desc())
                .distinct()
                .fetch();
    }

    // 비슷한 팝업 랜덤 추천
    @Override
    public List<PopupStore> findSimilarStores(Long categoryId, Long currentStoreId, int limit) {
        return queryFactory
                .selectFrom(store)
                .where(
                        isEndFalse(),
                        store.storeCategory.id.eq(categoryId),
                        store.id.ne(currentStoreId)  // 현재 스토어 제외
                )
                .orderBy(Expressions.numberTemplate(Double.class, "function('rand')").asc())  // 랜덤 정렬
                .limit(limit)
                .fetch();
    }

    // 조건식들
    private BooleanExpression isEndFalse() {
        return store.isEnd.eq(false);
    }

    private BooleanExpression dateEquals(LocalDate date) {
        if(date == null) return null;
        return store.startDate.loe(date)
                .and(store.endDate.goe(date));
    }

    private BooleanExpression nameContains(String name) {
        return name != null ? store.name.contains(name) : null;
    }

    private BooleanExpression createTimeAfter(LocalDateTime fromDate) {
        return store.createTime.goe(fromDate);
    }

    private BooleanExpression locationIn(List<String> locations) {
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        return store.location.in(locations);
    }

    private BooleanExpression ratingGoe(Double rating) {
        return rating != null ? store.rating.goe(rating) : null;
    }

    private BooleanExpression categoryIn(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;
        return store.storeCategory.id.in(categoryIds);
    }
}
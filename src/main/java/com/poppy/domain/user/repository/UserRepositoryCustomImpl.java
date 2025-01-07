package com.poppy.domain.user.repository;

import com.poppy.domain.popupStore.entity.PopupStoreView;
import com.poppy.domain.popupStore.entity.QPopupStoreView;
import com.poppy.domain.user.dto.response.UserPopupStoreRspDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QPopupStoreView view = QPopupStoreView.popupStoreView;

    @Override
    public List<UserPopupStoreRspDto> findRecentViewedStores(Long userId, int limit) {
        List<PopupStoreView> views = queryFactory
                .selectFrom(view)
                .join(view.user).fetchJoin()
                .join(view.popupStore).fetchJoin()
                .leftJoin(view.popupStore.images).fetchJoin()
                .where(view.user.id.eq(userId))
                .orderBy(view.viewedAt.desc())
                .distinct()
                .fetch();

        return views.stream()
                .map(view -> UserPopupStoreRspDto.of(view.getUser(), view.getPopupStore()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                UserPopupStoreRspDto::getPopupStoreId,  // 팝업스토어 ID로 중복 제거
                                Function.identity(),                     // DTO 자체를 값으로 사용
                                (existing, replacement) -> existing      // 중복 시 기존 값 유지 (최근 순으로 정렬되어 있으므로 첫 번째가 가장 최근)
                        ),
                        map -> new ArrayList<>(map.values())
                ))
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}

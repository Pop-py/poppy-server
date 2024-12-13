package com.poppy.domain.waiting.repository;

import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface WaitingRepository extends JpaRepository<Waiting, Long> {
    // 사용자의 중복 대기 여부 확인
    boolean existsByPopupStoreIdAndUserIdAndStatusIn(Long storeId, Long userId, Set<WaitingStatus> statuses);

    // 현재 대기 중인 인원 수 카운트
    long countByPopupStoreIdAndStatusIn(Long popupStoreId, Set<WaitingStatus> statuses);

    // 현재 최대 대기번호 조회 (다음 대기번호 생성용)
    @Query("SELECT MAX(w.waitingNumber) FROM Waiting w WHERE w.popupStore.id = :storeId")
    Optional<Integer> findMaxWaitingNumberByStoreId(@Param("storeId") Long storeId);

    // 특정 대기번호보다 큰 대기번호를 가진 대기자들 조회
    List<Waiting> findByPopupStoreIdAndStatusAndWaitingNumberGreaterThanOrderByWaitingNumberAsc(
            Long popupStoreId,
            WaitingStatus status,
            Integer waitingNumber
    );

    // 날짜별 대기 목록 조회 추가
    @Query("SELECT w FROM Waiting w " +
            "WHERE w.popupStore.id = :storeId " +
            "AND DATE(w.createTime) = DATE(:date) " +
            "ORDER BY w.createTime DESC")
    List<Waiting> findWaitingsByStoreIdAndDate(
            @Param("storeId") Long storeId,
            @Param("date") LocalDateTime date
    );

    // 활성화된 대기 목록 조회 추가
    @Query("SELECT w FROM Waiting w " +
            "WHERE w.popupStore.id = :storeId " +
            "AND w.status IN :activeStatuses " +
            "ORDER BY w.waitingNumber ASC")
    List<Waiting> findActiveWaitings(
            @Param("storeId") Long storeId,
            @Param("activeStatuses") Set<WaitingStatus> activeStatuses
    );

    // 특정 대기번호 앞에 있는 대기자 수 조회
    @Query("SELECT COUNT(w) FROM Waiting w " +
            "WHERE w.popupStore.id = :storeId " +
            "AND w.status IN :statuses " +
            "AND w.waitingNumber < :currentNumber")
    int countPeopleAhead(@Param("storeId") Long storeId, @Param("currentNumber") Integer currentNumber, @Param("statuses") Set<WaitingStatus> statuses);

    List<Waiting> findByStatus(WaitingStatus status);

    List<Waiting> findByUserIdOrderByCreateTimeDesc(Long userId);
}

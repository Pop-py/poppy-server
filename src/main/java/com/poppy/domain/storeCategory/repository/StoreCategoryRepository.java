package com.poppy.domain.storeCategory.repository;

import com.poppy.domain.storeCategory.entity.StoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory,Long> {
    Optional<StoreCategory> findByName(String name);
}

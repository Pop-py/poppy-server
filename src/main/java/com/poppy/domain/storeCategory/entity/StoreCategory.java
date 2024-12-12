package com.poppy.domain.storeCategory.entity;

import com.poppy.domain.popupStore.entity.PopupStore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "popup_store_categories")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "storeCategory")
    private List<PopupStore> popupStores = new ArrayList<>();
}

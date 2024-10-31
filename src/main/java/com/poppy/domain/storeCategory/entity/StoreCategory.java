package com.poppy.domain.storeCategory.entity;

import com.poppy.domain.popupStore.entity.PopupStore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "popup_store_categories")
public class StoreCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "storeCategory")
    private List<PopupStore> popupStores = new ArrayList<>();
}

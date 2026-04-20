package com.ecom.inventory.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    private String productId;

    @Column(nullable = false)
    private Integer availableQty;
}

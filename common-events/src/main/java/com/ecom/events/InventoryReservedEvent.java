package com.ecom.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent {
    private String eventId;
    private Long orderId;
    private String productId;
    private Integer quantity;
    private BigDecimal amount;
}

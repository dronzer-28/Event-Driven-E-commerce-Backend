package com.ecom.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFailedEvent {
    private String eventId;
    private Long orderId;
    private String productId;
    private String reason;
}

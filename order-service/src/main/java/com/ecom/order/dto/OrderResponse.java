package com.ecom.order.dto;

import com.ecom.order.model.Order;
import com.ecom.order.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String productId;
    private Integer quantity;
    private BigDecimal amount;
    private OrderStatus status;
    private Instant createdAt;

    public static OrderResponse from(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getProductId(),
                o.getQuantity(),
                o.getAmount(),
                o.getStatus(),
                o.getCreatedAt()
        );
    }
}

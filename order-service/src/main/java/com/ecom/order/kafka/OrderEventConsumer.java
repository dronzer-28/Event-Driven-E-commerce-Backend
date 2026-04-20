package com.ecom.order.kafka;

import com.ecom.events.InventoryFailedEvent;
import com.ecom.events.PaymentCompletedEvent;
import com.ecom.events.PaymentFailedEvent;
import com.ecom.events.Topics;
import com.ecom.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = Topics.INVENTORY_FAILED,
            groupId = "order-service",
            properties = { "spring.json.value.default.type=com.ecom.events.InventoryFailedEvent" }
    )
    public void onInventoryFailed(InventoryFailedEvent event) {
        log.info("Received InventoryFailed: orderId={} reason={}", event.getOrderId(), event.getReason());
        orderService.cancel(event.getOrderId(), "Inventory failed: " + event.getReason());
    }

    @KafkaListener(
            topics = Topics.PAYMENT_COMPLETED,
            groupId = "order-service",
            properties = { "spring.json.value.default.type=com.ecom.events.PaymentCompletedEvent" }
    )
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompleted: orderId={}", event.getOrderId());
        orderService.confirm(event.getOrderId());
    }

    @KafkaListener(
            topics = Topics.PAYMENT_FAILED,
            groupId = "order-service",
            properties = { "spring.json.value.default.type=com.ecom.events.PaymentFailedEvent" }
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailed: orderId={} reason={}", event.getOrderId(), event.getReason());
        orderService.cancel(event.getOrderId(), "Payment failed: " + event.getReason());
    }
}

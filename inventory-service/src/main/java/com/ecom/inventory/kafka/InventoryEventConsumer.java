package com.ecom.inventory.kafka;

import com.ecom.events.OrderCreatedEvent;
import com.ecom.events.PaymentFailedEvent;
import com.ecom.events.Topics;
import com.ecom.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = Topics.ORDER_CREATED,
            groupId = "inventory-service",
            properties = { "spring.json.value.default.type=com.ecom.events.OrderCreatedEvent" }
    )
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreated: orderId={} productId={} qty={}",
                event.getOrderId(), event.getProductId(), event.getQuantity());
        inventoryService.reserve(event);
    }

    @KafkaListener(
            topics = Topics.PAYMENT_FAILED,
            groupId = "inventory-service",
            properties = { "spring.json.value.default.type=com.ecom.events.PaymentFailedEvent" }
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailed: orderId={} - releasing stock", event.getOrderId());
        inventoryService.release(event);
    }
}

package com.ecom.notification.kafka;

import com.ecom.events.OrderCancelledEvent;
import com.ecom.events.OrderConfirmedEvent;
import com.ecom.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    @KafkaListener(
            topics = Topics.ORDER_CONFIRMED,
            groupId = "notification-service",
            properties = { "spring.json.value.default.type=com.ecom.events.OrderConfirmedEvent" }
    )
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("NOTIFICATION: Order {} confirmed. Emailing customer...", event.getOrderId());
    }

    @KafkaListener(
            topics = Topics.ORDER_CANCELLED,
            groupId = "notification-service",
            properties = { "spring.json.value.default.type=com.ecom.events.OrderCancelledEvent" }
    )
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("NOTIFICATION: Order {} cancelled. Reason: {}. Emailing customer...",
                event.getOrderId(), event.getReason());
    }
}

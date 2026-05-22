package com.ecom.notification.kafka;

import com.ecom.events.OrderCancelledEvent;
import com.ecom.events.OrderConfirmedEvent;
import com.ecom.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class NotificationConsumer {

    private static final int MAX_DEDUP_ENTRIES = 10_000;

    private final Set<String> processedEventIds = Collections.newSetFromMap(
            new LinkedHashMap<>(MAX_DEDUP_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_DEDUP_ENTRIES;
                }
            });

    @KafkaListener(
            topics = Topics.ORDER_CONFIRMED,
            groupId = "notification-service",
            properties = { "spring.json.value.default.type=com.ecom.events.OrderConfirmedEvent" }
    )
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        if (event.getEventId() != null && !processedEventIds.add(event.getEventId())) {
            log.warn("Duplicate event ignored: eventId={}", event.getEventId());
            return;
        }
        log.info("NOTIFICATION: Order {} confirmed. Emailing customer...", event.getOrderId());
    }

    @KafkaListener(
            topics = Topics.ORDER_CANCELLED,
            groupId = "notification-service",
            properties = { "spring.json.value.default.type=com.ecom.events.OrderCancelledEvent" }
    )
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (event.getEventId() != null && !processedEventIds.add(event.getEventId())) {
            log.warn("Duplicate event ignored: eventId={}", event.getEventId());
            return;
        }
        log.info("NOTIFICATION: Order {} cancelled. Reason: {}. Emailing customer...",
                event.getOrderId(), event.getReason());
    }
}

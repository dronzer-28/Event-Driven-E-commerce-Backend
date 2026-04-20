package com.ecom.order.kafka;

import com.ecom.events.OrderCancelledEvent;
import com.ecom.events.OrderConfirmedEvent;
import com.ecom.events.OrderCreatedEvent;
import com.ecom.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreated: orderId={}", event.getOrderId());
        kafkaTemplate.send(Topics.ORDER_CREATED, String.valueOf(event.getOrderId()), event);
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Publishing OrderConfirmed: orderId={}", event.getOrderId());
        kafkaTemplate.send(Topics.ORDER_CONFIRMED, String.valueOf(event.getOrderId()), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Publishing OrderCancelled: orderId={} reason={}", event.getOrderId(), event.getReason());
        kafkaTemplate.send(Topics.ORDER_CANCELLED, String.valueOf(event.getOrderId()), event);
    }
}

package com.ecom.inventory.kafka;

import com.ecom.events.InventoryFailedEvent;
import com.ecom.events.InventoryReservedEvent;
import com.ecom.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishReserved(InventoryReservedEvent event) {
        log.info("Publishing InventoryReserved: orderId={} productId={}", event.getOrderId(), event.getProductId());
        kafkaTemplate.send(Topics.INVENTORY_RESERVED, String.valueOf(event.getOrderId()), event);
    }

    public void publishFailed(InventoryFailedEvent event) {
        log.info("Publishing InventoryFailed: orderId={} productId={} reason={}",
                event.getOrderId(), event.getProductId(), event.getReason());
        kafkaTemplate.send(Topics.INVENTORY_FAILED, String.valueOf(event.getOrderId()), event);
    }
}

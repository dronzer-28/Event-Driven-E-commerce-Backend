package com.ecom.payment.kafka;

import com.ecom.events.InventoryReservedEvent;
import com.ecom.events.Topics;
import com.ecom.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = Topics.INVENTORY_RESERVED,
            groupId = "payment-service",
            properties = { "spring.json.value.default.type=com.ecom.events.InventoryReservedEvent" }
    )
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReserved: orderId={} amount={}", event.getOrderId(), event.getAmount());
        paymentService.process(event);
    }
}

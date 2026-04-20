package com.ecom.payment.kafka;

import com.ecom.events.PaymentCompletedEvent;
import com.ecom.events.PaymentFailedEvent;
import com.ecom.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCompleted(PaymentCompletedEvent event) {
        log.info("Publishing PaymentCompleted: orderId={} paymentId={}", event.getOrderId(), event.getPaymentId());
        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, String.valueOf(event.getOrderId()), event);
    }

    public void publishFailed(PaymentFailedEvent event) {
        log.info("Publishing PaymentFailed: orderId={} reason={}", event.getOrderId(), event.getReason());
        kafkaTemplate.send(Topics.PAYMENT_FAILED, String.valueOf(event.getOrderId()), event);
    }
}

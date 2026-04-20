package com.ecom.inventory.service;

import com.ecom.events.InventoryFailedEvent;
import com.ecom.events.InventoryReservedEvent;
import com.ecom.events.OrderCreatedEvent;
import com.ecom.events.PaymentFailedEvent;
import com.ecom.inventory.kafka.InventoryEventProducer;
import com.ecom.inventory.model.InventoryItem;
import com.ecom.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer producer;

    @Transactional
    public void reserve(OrderCreatedEvent event) {
        Optional<InventoryItem> maybe = inventoryRepository.findByProductIdForUpdate(event.getProductId());

        if (maybe.isEmpty()) {
            producer.publishFailed(new InventoryFailedEvent(
                    event.getOrderId(), event.getProductId(), "Product not found"));
            return;
        }

        InventoryItem item = maybe.get();
        if (item.getAvailableQty() < event.getQuantity()) {
            producer.publishFailed(new InventoryFailedEvent(
                    event.getOrderId(), event.getProductId(),
                    "Insufficient stock (have=" + item.getAvailableQty() + ", need=" + event.getQuantity() + ")"));
            return;
        }

        item.setAvailableQty(item.getAvailableQty() - event.getQuantity());
        inventoryRepository.save(item);
        log.info("Reserved {} of {}: remaining={}", event.getQuantity(), event.getProductId(), item.getAvailableQty());

        producer.publishReserved(new InventoryReservedEvent(
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity(),
                event.getAmount()
        ));
    }

    @Transactional
    public void release(PaymentFailedEvent event) {
        Optional<InventoryItem> maybe = inventoryRepository.findByProductIdForUpdate(event.getProductId());
        if (maybe.isEmpty()) {
            log.warn("Cannot release stock, product missing: {}", event.getProductId());
            return;
        }
        InventoryItem item = maybe.get();
        item.setAvailableQty(item.getAvailableQty() + event.getQuantity());
        inventoryRepository.save(item);
        log.info("Released {} of {} (orderId={}): available={}",
                event.getQuantity(), event.getProductId(), event.getOrderId(), item.getAvailableQty());
    }
}

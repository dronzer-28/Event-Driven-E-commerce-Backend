package com.ecom.inventory.config;

import com.ecom.inventory.model.InventoryItem;
import com.ecom.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final InventoryRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Inventory already seeded, skipping.");
            return;
        }
        List<InventoryItem> seed = List.of(
                InventoryItem.builder().productId("P1").availableQty(100).build(),
                InventoryItem.builder().productId("P2").availableQty(50).build(),
                InventoryItem.builder().productId("P3").availableQty(10).build(),
                InventoryItem.builder().productId("P4").availableQty(5).build(),
                InventoryItem.builder().productId("OUT").availableQty(0).build()
        );
        repository.saveAll(seed);
        log.info("Seeded {} inventory items.", seed.size());
    }
}

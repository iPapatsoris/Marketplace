package com.marketplace.invetory.reservation.scheduled;

import com.marketplace.invetory.reservation.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class InventoryReservationExpirationJob {
        private final InventoryReservationRepository repository;

        @Scheduled(fixedDelay = 60_000) // every minute
        @Transactional
        public void expireReservations() {
            System.out.println("hi");
            repository.expireReservations(Instant.now());
        }
    }

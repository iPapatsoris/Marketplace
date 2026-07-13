package com.marketplace.reservation.scheduled;

import com.marketplace.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationExpirationJob {
        private final ReservationRepository repository;

        @Scheduled(fixedDelay = 60_000) // every minute
        @Transactional
        public void expireReservations() {
            repository.expireReservations();
        }
    }

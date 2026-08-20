package com.marketplace.reservation.scheduled;

import com.marketplace.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "scheduler.enabled",
        havingValue = "true"
)
public class ReservationExpirationJob {
        private final ReservationService reservationService;

        @Scheduled(fixedDelay = 60_000) // every minute
        public void expireReservations() {
               reservationService.expireReservations();
            }
        }

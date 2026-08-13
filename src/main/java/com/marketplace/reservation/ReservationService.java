package com.marketplace.reservation;

import com.marketplace.product.Product;
import com.marketplace.reservation.dto.CreateReservationRequest;
import com.marketplace.reservation.dto.CreateReservationResponse;
import com.marketplace.reservation.dto.ReservationResponse;
import com.marketplace.reservation.entity.Reservation;
import com.marketplace.reservation.entity.ReservationFactory;
import com.marketplace.reservation.exception.InsufficientStockException;
import com.marketplace.product.ProductSnapshot;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.exception.ProductNotFoundException;
import com.marketplace.reservation.exception.ReservationNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationMapper reservationMapper;
    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final ReservationFactory reservationFactory;
    private final Clock clock;

    /**
     * Reserves a quantity of the selected product. Protected against race conditions with optimistic locking.
     * If product has changed since the user viewed it, abort transaction.
     * A snapshot of the product is captured on reservation, to ensure the user will get what they signed up for,
     * even if the product changes later.
     */
    @Transactional
    public CreateReservationResponse createReservation(Long productID, CreateReservationRequest dto) {
        Optional<ProductSnapshot> optionalProductSnapshot = productRepository.reserveInventory(productID, dto.productVersion(), dto.quantity());
        if (optionalProductSnapshot.isEmpty()) {
            if (!productRepository.existsById(productID)) {
                throw new ProductNotFoundException(productID);
            }
            if (!productRepository.existsByIdAndVersion(productID, dto.productVersion())) {
                throw new OptimisticLockException("Product with id %d has changed".formatted(productID));
            }

            Optional<Integer> inventoryByIdOptional = productRepository.findInventoryById(productID);
            throw new InsufficientStockException(productID, dto.quantity(), inventoryByIdOptional.orElse(null));
        }
        String productSnapshotAsString = objectMapper.writeValueAsString(optionalProductSnapshot.get());
        Product productRef = productRepository.getReferenceById(productID);
        Reservation reservation = reservationFactory.create(dto, productSnapshotAsString, productRef);

        reservationRepository.save(reservation);

        return reservationMapper.toCreateReservationResponse(reservation);
    }

    @Transactional
    ReservationResponse getReservation(Long reservationId) {
        Optional<Reservation> optionalReservation = reservationRepository.findById(reservationId);
        if (optionalReservation.isEmpty()) {
            throw new ReservationNotFoundException(reservationId);
        }

        return reservationMapper.toReservationResponse(optionalReservation.get());
    }

    @Transactional
    public void expireReservations() {
        List<Reservation> expiredReservations = reservationRepository.expireReservations(Instant.now(clock));
        for (Reservation expiredReservation : expiredReservations) {
            // N + 1 queries
            productRepository.increaseInventory(expiredReservation.getId(), expiredReservation.getQuantity());
        }
    }
}

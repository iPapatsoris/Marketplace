package com.marketplace.reservation;

import com.marketplace.product.Product;
import com.marketplace.product.ProductBuilder;
import com.marketplace.product.ProductSnapshot;
import com.marketplace.product.exception.ProductNotFoundException;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.reservation.dto.CreateReservationRequest;
import com.marketplace.reservation.dto.CreateReservationResponse;
import com.marketplace.reservation.dto.ReservationResponse;
import com.marketplace.reservation.entity.Reservation;
import com.marketplace.reservation.entity.ReservationBuilder;
import com.marketplace.reservation.entity.ReservationFactory;
import com.marketplace.reservation.exception.InsufficientStockException;
import com.marketplace.reservation.exception.ReservationNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    private ReservationService reservationService;
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ProductRepository productRepository;

    private final ReservationMapper reservationMapper = Mappers.getMapper(ReservationMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReservationFactory reservationFactory = new ReservationFactory(fixedClock, reservationMapper);

    @BeforeEach
    void setup() {
        reservationService = new ReservationService(reservationMapper, reservationRepository, productRepository, objectMapper, reservationFactory, fixedClock);
    }

    @Nested
    class CreateReservationTest {
        private final Long productId = 1L;
        private final int quantityRequest = 5;
        private final Long productVersionRequest = 10L;

        @Test
        void shouldCreateReservation() {
            CreateReservationRequest reservationRequest = new CreateReservationRequest(
                    quantityRequest, productVersionRequest
            );

            Long newReservationId = 30L;
            CreateReservationResponse expectedResponse = new CreateReservationResponse(
                    newReservationId, quantityRequest, fixedClock.instant().plus(10, MINUTES)
            );

             Product  product = new ProductBuilder()
                     .withId(productId)
                     .withName("chair")
                     .withPrice(new BigDecimal(35))
                     .build();

            ProductSnapshot productSnapshot = new ProductSnapshot(product.getName(), product.getPrice());

            when(productRepository.reserveInventory(
                    productId, productVersionRequest, quantityRequest
            )).thenReturn(Optional.of(productSnapshot));

            when(productRepository.getReferenceById(productId)).thenReturn(product);

            when(reservationRepository.save(any(Reservation.class))).thenAnswer(
                    invocationOnMock -> {
                        Reservation reservation = invocationOnMock.getArgument(0);
                        reservation.setId(newReservationId);
                        return reservation;
                    }
            );

            CreateReservationResponse response = reservationService.createReservation(productId, reservationRequest);
            assertEquals(expectedResponse, response);
            verify(reservationRepository).save(any(Reservation.class));
        }

        @Test
        void shouldThrowProductNotFoundExceptionWhenProductDoesNotExist() {
            CreateReservationRequest reservationRequest = new CreateReservationRequest(
                    quantityRequest, productVersionRequest
            );

            when(productRepository.reserveInventory(productId, productVersionRequest, quantityRequest))
                    .thenReturn(Optional.empty());
            when(productRepository.existsById(productId))
                    .thenReturn(false);

            assertThrowsExactly(ProductNotFoundException.class, () ->
                    reservationService.createReservation(productId, reservationRequest));
        }

        @Test
        void shouldThrowOptimisticLockExceptionWhenProductVersionMismatch() {
            CreateReservationRequest reservationRequest = new CreateReservationRequest(
                    quantityRequest, productVersionRequest
            );

            when(productRepository.reserveInventory(productId, productVersionRequest, quantityRequest))
                    .thenReturn(Optional.empty());
            when(productRepository.existsById(productId))
                    .thenReturn(true);
            when(productRepository.existsByIdAndVersion(productId, productVersionRequest))
                    .thenReturn(false);

            assertThrowsExactly(OptimisticLockException.class, () ->
                    reservationService.createReservation(productId, reservationRequest));

        }

        @Test
        void shouldThrowInsufficientStockExceptionWhenQuantityExceedsStock() {
            CreateReservationRequest reservationRequest = new CreateReservationRequest(
                    quantityRequest, productVersionRequest
            );

            when(productRepository.reserveInventory(productId, productVersionRequest, quantityRequest))
                    .thenReturn(Optional.empty());
            when(productRepository.existsById(productId))
                    .thenReturn(true);
            when(productRepository.existsByIdAndVersion(productId, productVersionRequest))
                    .thenReturn(true);

            assertThrowsExactly(InsufficientStockException.class, () ->
                    reservationService.createReservation(productId, reservationRequest));

        }
    }

    @Nested
    class GetReservationTest {

        @Test
        void shouldReturnReservationWhenItExists() {
            Long id = 1L;
            Reservation storedReservation =  new ReservationBuilder()
                    .withId(id)
                    .withOrderId(5L)
                    .build();

            ReservationResponse expectedResponse = new ReservationResponse(
                    storedReservation.getId(), storedReservation.getStatus(), storedReservation.getOrderId());

            when(reservationRepository.findById(id))
                    .thenReturn(Optional.of(storedReservation));

            ReservationResponse response = reservationService.getReservation(id);

            assertEquals(expectedResponse, response);
        }

        @Test
        void shouldThrowReservationNotFoundExceptionWhenReservationDoesNotExist() {
            Long id = 1L;

            when(reservationRepository.findById(id))
                    .thenReturn(Optional.empty());

            assertThrowsExactly(ReservationNotFoundException.class, () -> reservationService.getReservation(id));
        }

    }

}

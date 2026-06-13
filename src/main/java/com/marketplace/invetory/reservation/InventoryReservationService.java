package com.marketplace.invetory.reservation;

import com.marketplace.invetory.reservation.dto.CreateInventoryReservationRequest;
import com.marketplace.invetory.reservation.dto.CreateInventoryReservationResponse;
import com.marketplace.invetory.reservation.exception.InsufficientStockException;
import com.marketplace.product.ProductSnapshot;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.exception.ProductNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryReservationService {
    private final InventoryReservationMapper inventoryReservationMapper;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final ProductRepository productRepository;

    /**
     * Reserves a quantity of the selected product. Protected against race conditions with optimistic locking.
     * If product has changed since the user viewed it, abort transaction.
     * A snapshot of the product is captured on reservation, to ensure the user will get what they signed up for,
     * even if the product changes later.
     */
    @Transactional
    public CreateInventoryReservationResponse createInventoryReservation(Long productID, CreateInventoryReservationRequest dto) {
        Optional<ProductSnapshot> optionalProductSnapshot = productRepository.reserveInventory(productID, dto.productVersion(), dto.quantity());
        if (optionalProductSnapshot.isEmpty()) {
            if (!productRepository.existsById(productID)) {
                throw new ProductNotFoundException(productID);
            }
            if (!productRepository.existsByIdAndVersion(productID, dto.productVersion())) {
                throw new OptimisticLockException("Product with id %d has changed".formatted(productID));
            }
            throw new InsufficientStockException(productID, dto.quantity(), productRepository.findInventoryById(productID).get());
        }

        InventoryReservation reservation = inventoryReservationMapper.toEntity(dto);
        reservation.setProductSnapshot(optionalProductSnapshot.get());
        reservation.setProduct(productRepository.getReferenceById(productID));
        inventoryReservationRepository.save(reservation);

        return inventoryReservationMapper.toCreateInventoryReservationResponse(reservation);
    }
}

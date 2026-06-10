package com.marketplace.invetory.reservation;

import com.marketplace.invetory.reservation.dto.CreateInventoryReservationRequest;
import com.marketplace.invetory.reservation.dto.CreateInventoryReservationResponse;
import com.marketplace.invetory.reservation.exception.InsufficientStockException;
import com.marketplace.product.ProductRepository;
import com.marketplace.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryReservationService {
    private final InventoryReservationMapper inventoryReservationMapper;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CreateInventoryReservationResponse createInventoryReservation(Long productID, CreateInventoryReservationRequest dto) {
        int updated = productRepository.reserveInventory(productID, dto.quantity());
        if (updated == 0) {
            if (!productRepository.existsById(productID)) {
                throw new ProductNotFoundException(productID);
            }
            throw new InsufficientStockException(productID, dto.quantity(), productRepository.findInventoryByID(productID).get());
        }

        InventoryReservation reservation = inventoryReservationMapper.toEntity(dto);
        inventoryReservationRepository.save(reservation);

        reservation.setProduct(productRepository.getReferenceById(productID));

        return inventoryReservationMapper.toCreateInventoryReservationResponse(reservation);
    }
}

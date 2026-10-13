package com.example.inventory.service;

import com.example.inventory.dto.*;
import com.example.inventory.entity.InventoryReservation;
import com.example.inventory.entity.Product;
import com.example.inventory.entity.ReservationItem;
import com.example.inventory.dto.OrderStatus;
import com.example.inventory.repository.InventoryReservationRepo;
import com.example.inventory.repository.ProductsRepo;
import com.example.inventory.repository.ReservationItemRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class InventoryService {

    private final ProductsRepo productsRepo;
    private final ReservationItemRepo reservationItemRepo;
    private final InventoryReservationRepo inventoryReservationRepo;

    public InventoryService(
            ProductsRepo productsRepo,
            ReservationItemRepo reservationItemRepo,
            InventoryReservationRepo inventoryReservationRepo) {
        this.productsRepo = productsRepo;
        this.reservationItemRepo = reservationItemRepo;
        this.inventoryReservationRepo = inventoryReservationRepo;
    }

    // Add a product
    @Transactional
    public ProductDTO addProduct(ProductDTO productDTO) {
        Product product = productsRepo.save(productDTO.toEntity());
        return product.toDTO();
    }

    // Get all products
    @Transactional(readOnly = true)
    public List<ProductDTO> getProducts() {
        return productsRepo.findAll()
                .stream()
                .map(Product::toDTO)
                .toList();
    }

    // Reserve stock for an order
    @Transactional
    public OrderDTO reserveProducts(OrderDTO orderDTO) {

        if (orderDTO.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }

        List<RequestedProductDTO> requestedProducts =
                orderDTO.getRequestedProducts();

        if (requestedProducts == null || requestedProducts.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one product is required");
        }

        // Aggregate duplicate product IDs and validate quantities.
        // TreeMap also gives a consistent product-locking order.
        Map<Long, Integer> quantities = new TreeMap<>();

        for (RequestedProductDTO requested : requestedProducts) {
            if (requested.getProductId() == null
                    || requested.getProductId() <= 0
                    || requested.getQuantity() == null
                    || requested.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Valid product IDs and positive quantities are required");
            }

            quantities.merge(
                    requested.getProductId(),
                    requested.getQuantity(),
                    Math::addExact
            );
        }

        // Quick duplicate check.
        Optional<InventoryReservation> existing =
                inventoryReservationRepo.findByOrderId(orderDTO.getOrderId());

        if (existing.isPresent()) {
            return handleExistingReservation(orderDTO, existing.get());
        }

        // Lock all existing products in a consistent order.
        Map<Long, Product> products = new LinkedHashMap<>();

        for (Long productId : quantities.keySet()) {
            productsRepo.findByIdForUpdate(productId)
                    .ifPresent(product -> products.put(productId, product));
        }

        // Recheck after acquiring product locks.
        // Another request might have processed this order while we waited.
        existing = inventoryReservationRepo.findByOrderId(
                orderDTO.getOrderId());

        if (existing.isPresent()) {
            return handleExistingReservation(orderDTO, existing.get());
        }

        // Check all requested quantities before changing any stock.
        List<StockReservationResultDTO> results = new ArrayList<>();
        boolean sufficient = true;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();

            Product product = products.get(productId);

            if (product == null) {
                results.add(new StockReservationResultDTO(
                        productId,
                        requestedQuantity,
                        0,
                        false,
                        "Product not found"
                ));

                sufficient = false;
                continue;
            }

            Integer availableQuantity = product.getAvailableQuantity();

            boolean available = availableQuantity != null
                    && availableQuantity >= requestedQuantity;

            results.add(new StockReservationResultDTO(
                    productId,
                    requestedQuantity,
                    availableQuantity,
                    available,
                    available
                            ? "Stock available"
                            : "Insufficient stock"
            ));

            if (!available) {
                sufficient = false;
            }
        }

        orderDTO.setReservationResults(results);

        // Do not reserve partially if any product fails.
        if (!sufficient) {
            orderDTO.setOrderStatus(OrderStatus.NOT_SUFFICIENT);
            return orderDTO;
        }

        // Create the reservation before changing stock.
        // The database must enforce UNIQUE(order_id).
        InventoryReservation reservation =
                InventoryReservation.builder()
                        .orderId(orderDTO.getOrderId())
                        .status(OrderStatus.RESERVED)
                        .build();

        reservation = inventoryReservationRepo.saveAndFlush(reservation);

        // Save the quantities needed to restore stock on cancellation.
        List<ReservationItem> items = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            items.add(ReservationItem.builder()
                    .reservation(reservation)
                    .productId(entry.getKey())
                    .quantity(entry.getValue())
                    .build());
        }

        reservationItemRepo.saveAll(items);

        // Deduct stock only after every product passes validation.
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());

            product.setAvailableQuantity(
                    product.getAvailableQuantity() - entry.getValue()
            );
        }

        // Managed entities are updated automatically at transaction commit.
        productsRepo.saveAll(products.values());

        orderDTO.setOrderStatus(OrderStatus.RESERVED);
        return orderDTO;
    }

    // Cancel a reservation and restore its stock
    @Transactional
    public String cancelReservation(Long orderId) {

        InventoryReservation reservation =
                inventoryReservationRepo.findByOrderIdForUpdate(orderId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Reservation not found for order: " + orderId));

        // Prevent restoring the same stock twice.
        if (reservation.getStatus() == OrderStatus.RELEASED) {
            return "Reservation already cancelled";
        }

        if (reservation.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException(
                    "Cannot cancel reservation with status: "
                            + reservation.getStatus());
        }

        List<ReservationItem> items =
                reservationItemRepo.findByReservation_Id(reservation.getId());

        // Aggregate and sort product IDs to reduce deadlock risk.
        Map<Long, Integer> quantitiesToRestore = new TreeMap<>();

        for (ReservationItem item : items) {
            quantitiesToRestore.merge(
                    item.getProductId(),
                    item.getQuantity(),
                    Math::addExact
            );
        }

        Map<Long, Product> productsToUpdate = new LinkedHashMap<>();

        // Lock products before restoring stock.
        for (Long productId : quantitiesToRestore.keySet()) {
            Product product = productsRepo.findByIdForUpdate(productId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Product not found: " + productId));

            productsToUpdate.put(productId, product);
        }

        // Restore stock.
        for (Map.Entry<Long, Integer> entry
                : quantitiesToRestore.entrySet()) {

            Product product = productsToUpdate.get(entry.getKey());

            product.setAvailableQuantity(
                    Math.addExact(
                            product.getAvailableQuantity(),
                            entry.getValue()
                    )
            );
        }

        productsRepo.saveAll(productsToUpdate.values());

        // Mark released in the same transaction as the stock restoration.
        reservation.setStatus(OrderStatus.RELEASED);

        return "Reservation cancelled successfully";
    }

    private OrderDTO handleExistingReservation(
            OrderDTO orderDTO,
            InventoryReservation reservation) {

        if (reservation.getStatus() == OrderStatus.RESERVED) {
            orderDTO.setOrderStatus(OrderStatus.RESERVED);
            return orderDTO;
        }

        if (reservation.getStatus() == OrderStatus.RELEASED) {
            throw new IllegalStateException(
                    "This order's reservation has already been released");
        }

        throw new IllegalStateException(
                "Order already has a reservation with status: "
                        + reservation.getStatus());
    }
}
package com.example.inventory.service;

import com.example.inventory.dto.*;
import com.example.inventory.entity.Product;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.repository.ProductsRepo;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {

    private final ProductsRepo productsRepo;
    public InventoryService(ProductsRepo productsRepo) {
        this.productsRepo=productsRepo;
    }

    public ProductDTO addProduct(ProductDTO productDTO){
        return productsRepo.save(productDTO.toEntity()).toDTO();
    }
    public List<ProductDTO> getProducts(){
        return productsRepo.findAll().stream().map(Product::toDTO).toList();
    }



    @Transactional
    public OrderDTO reserveProduct(OrderDTO orderDTO) {

        List<RequestedProductDTO> requestedProducts =
                orderDTO.getRequestedProducts();

        List<StockReservationResultDTO> results = new ArrayList<>();
        boolean sufficient = true;

        // 1. Check availability of all requested products
        for (RequestedProductDTO requested : requestedProducts) {

            Long id = requested.getProductId();

            Product product = productsRepo.findById(id)
                    .orElse(null);

            if (product == null) {
                results.add(new StockReservationResultDTO(
                        id,
                        requested.getQuantity(),
                        0,
                        false,
                        "Product not found"
                ));

                sufficient = false;
                continue;
            }

            boolean available =
                    product.getAvailableQuantity() >= requested.getQuantity();

            results.add(new StockReservationResultDTO(
                    id,
                    requested.getQuantity(),
                    product.getAvailableQuantity(),
                    available,
                    available ? "Stock available" : "Insufficient stock"
            ));

            if (!available) {
                sufficient = false;
            }
        }

        // 2. Return without reserving anything if any product fails
        if (!sufficient) {
            orderDTO.setOrderStatus(OrderStatus.NOT_SUFFICIENT);
            orderDTO.setReservationResults(results);
            return orderDTO;
        }

        // 3. Reserve stock only after all checks pass
        for (RequestedProductDTO requested : requestedProducts) {

            Product product = productsRepo.findById(
                    requested.getProductId()
            ).orElseThrow();

            product.setAvailableQuantity(
                    product.getAvailableQuantity() - requested.getQuantity()
            );

            productsRepo.save(product);
        }

        orderDTO.setOrderStatus(OrderStatus.RESERVED);
        orderDTO.setReservationResults(results);

        return orderDTO;
    }
}

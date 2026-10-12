package com.example.inventory.service;

import com.example.inventory.dto.ProductDTO;
import com.example.inventory.entity.Product;
import com.example.inventory.repository.ProductsRepo;
import org.springframework.stereotype.Service;

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

}

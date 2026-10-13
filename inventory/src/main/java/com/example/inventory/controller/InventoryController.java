
package com.example.inventory.controller;

import com.example.inventory.dto.OrderDTO;
import com.example.inventory.dto.ProductDTO;
import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    public InventoryController(InventoryService inventoryService){
        this.inventoryService = inventoryService;
    }

    @GetMapping("/getallbils")
    public List<ProductDTO> getAllProducts() {
        return inventoryService.getProducts();
    }

    @PostMapping("/addproduct")
    public ProductDTO addProduct(@Valid @RequestBody ProductDTO productDTO) {

        return inventoryService.addProduct(productDTO);
    }
    @PostMapping("/reserve")
    public OrderDTO reserveProducts(@RequestBody OrderDTO orderDTO) {

        return inventoryService.reserveProducts(orderDTO);
    }
    @PostMapping("/cancel")
    public String cancelOrder(@RequestBody OrderDTO orderDTO) {

        return inventoryService.cancelReservation(orderDTO.getOrderId());
    }
}

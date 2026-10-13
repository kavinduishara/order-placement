package com.example.inventory.entity;

import com.example.inventory.dto.ProductDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer availableQuantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    private Integer reserved;

    public Product(String name, BigDecimal unitPrice, Integer availableQuantity) {
        this.name = name;
        this.unitPrice = unitPrice;
        this.availableQuantity = availableQuantity;
    }
    public ProductDTO toDTO(){

        return new ProductDTO(this.id,this.name,this.availableQuantity,this.unitPrice);
    }
}

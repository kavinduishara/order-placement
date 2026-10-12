
package com.example.inventory.dto;

import com.example.inventory.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ProductDTO {
    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Available quantity is required")
    @PositiveOrZero(message = "Quantity cannot be negative")
    private Integer availableQuantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;


    public Product toEntity(){
        return new Product(this.name,this.unitPrice,this.availableQuantity);
    }
}
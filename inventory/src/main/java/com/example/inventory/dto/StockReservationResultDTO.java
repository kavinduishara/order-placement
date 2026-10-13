package com.example.inventory.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationResultDTO {

    private Long productId;
    private Integer requestedQuantity;
    private Integer availableQuantity;
    private boolean sufficient;
    private String message;
}
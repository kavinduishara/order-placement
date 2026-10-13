package com.example.inventory.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long orderId;
    private List<RequestedProductDTO> requestedProducts;
    private OrderStatus orderStatus;
    private List<StockReservationResultDTO> reservationResults;
}
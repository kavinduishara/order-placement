package com.example.inventory.kafka;

import com.example.inventory.dto.OrderDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    @KafkaListener(
            topics = "order",
            groupId = "group"
    )
    public void receiveResponse(OrderDTO course) {

        String message = course.getOrderId() + " Got the order";

        System.out.println(message);
    }
}

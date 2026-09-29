package com.example.order.kafka;

import org.springframework.kafka.annotation.KafkaListener;

public class KafkaConsumer {

    @KafkaListener(
            topics = "inventory",
            groupId = "group"
    )
    public void receiveResponse(String course) {

        String message = course + " Got the response back from consumer";

        System.out.println(message);
    }
}

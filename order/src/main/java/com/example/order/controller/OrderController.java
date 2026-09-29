package com.example.order.controller;

import com.example.order.dto.OrderDTO;
import com.example.order.kafka.KafkaConsumer;
import com.example.order.kafka.KafkaProducer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class OrderController {

    private final KafkaProducer kafkaProducer;


    OrderController(KafkaProducer kafkaProducer){
        this.kafkaProducer=kafkaProducer;


    }
    @PostMapping("/place-order")
    public ResponseEntity<String> addCourse(@RequestBody OrderDTO orderDTO)
    {
        String response=kafkaProducer.sendMessage(orderDTO);
        return new ResponseEntity<String>(response, HttpStatus.OK);
    }
}

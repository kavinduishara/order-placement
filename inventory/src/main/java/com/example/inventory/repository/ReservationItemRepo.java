package com.example.inventory.repository;

import com.example.inventory.entity.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationItemRepo
        extends JpaRepository<ReservationItem, Long> {

    List<ReservationItem> findByReservation_Id(Long reservationId);
}
package com.example.inventory.repository;

import com.example.inventory.entity.InventoryReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryReservationRepo
        extends JpaRepository<InventoryReservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM InventoryReservation r
        WHERE r.orderId = :orderId
        """)
    Optional<InventoryReservation> findByOrderIdForUpdate(
            @Param("orderId") Long orderId
    );

    Optional<InventoryReservation> findByOrderId(Long orderId);
}
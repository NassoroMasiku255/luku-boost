package com.lukuboost.electricityloan.meter.repository;

import com.lukuboost.electricityloan.meter.entity.Meter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeterRepository extends JpaRepository<Meter, UUID> {

    Optional<Meter> findByMeterNumber(String meterNumber);

    boolean existsByMeterNumber(String meterNumber);

    List<Meter> findByCustomerId(UUID customerId);

    @Query("SELECT m FROM Meter m WHERE " +
            "(:meterNumber IS NULL OR m.meterNumber LIKE %:meterNumber%) AND " +
            "(:status IS NULL OR m.status = :status)")
    Page<Meter> searchMeters(
            @Param("meterNumber") String meterNumber,
            @Param("status") String status,
            Pageable pageable);
}

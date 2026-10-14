package com.lukuboost.electricityloan.otp.repository;

import com.lukuboost.electricityloan.otp.entity.OtpRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpRequestRepository extends JpaRepository<OtpRequest, Long> {

    Optional<OtpRequest> findByApplicationRefAndStatus(String applicationRef, String status);

    @Query("SELECT o FROM OtpRequest o WHERE o.applicationRef = :appRef ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpRequest> findLatestByApplicationRef(@Param("appRef") String applicationRef);

    @Query("SELECT COUNT(o) FROM OtpRequest o WHERE o.phoneNumber = :phone AND o.createdAt >= :since")
    long countByPhoneNumberSince(@Param("phone") String phoneNumber, @Param("since") Instant since);

    @Modifying
    @Query("UPDATE OtpRequest o SET o.status = 'SUPERSEDED' WHERE o.phoneNumber = :phone AND o.status = 'PENDING'")
    void supersedePendingByPhoneNumber(@Param("phone") String phoneNumber);

    @Query("SELECT o FROM OtpRequest o WHERE " +
            "(:phone IS NULL OR o.phoneNumber = :phone) AND " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:from IS NULL OR o.createdAt >= :from) AND " +
            "(:to IS NULL OR o.createdAt <= :to)")
    Page<OtpRequest> searchOtpRequests(
            @Param("phone") String phone,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT COUNT(o) FROM OtpRequest o WHERE o.createdAt >= :since")
    long countSince(@Param("since") Instant since);

    @Modifying
    @Query("UPDATE OtpRequest o SET o.status = 'EXPIRED' WHERE o.status = 'PENDING' AND o.expiresAt < :now")
    int expireOldOtps(@Param("now") Instant now);
}

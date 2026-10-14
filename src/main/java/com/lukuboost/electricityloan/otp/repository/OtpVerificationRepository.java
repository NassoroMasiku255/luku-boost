package com.lukuboost.electricityloan.otp.repository;

import com.lukuboost.electricityloan.otp.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
}

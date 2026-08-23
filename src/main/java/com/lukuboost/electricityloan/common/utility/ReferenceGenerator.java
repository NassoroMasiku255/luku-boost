package com.lukuboost.electricityloan.common.utility;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class ReferenceGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    private ReferenceGenerator() {}

    public static String generateLoanReference() {
        return "LOAN-" + LocalDate.now().format(DATE_FMT) + "-" + nextSequence();
    }

    public static String generateApplicationReference() {
        return "APP-" + LocalDate.now().format(DATE_FMT) + "-" + nextSequence();
    }

    public static String generateTokenReference() {
        return "TOKEN-" + LocalDate.now().format(DATE_FMT) + "-" + nextSequence();
    }

    public static String generateRepaymentReference() {
        return "PAY-" + LocalDate.now().format(DATE_FMT) + "-" + nextSequence();
    }

    public static String generateTransactionReference() {
        return "TXN-" + LocalDate.now().format(DATE_FMT) + "-" + nextSequence();
    }

    public static String generateReconReference() {
        return "RECON-" + LocalDate.now().format(DATE_FMT) + "-" + nextSequence();
    }

    public static String generateCustomerReference() {
        return "CUST-" + LocalDate.now().format(DATE_FMT) + "-" + nextSequence();
    }

    public static String generateErrorReference() {
        return "ERR-" + LocalDate.now().format(DATE_FMT) + "-" + nextSequence();
    }

    public static String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String nextSequence() {
        return String.format("%09d", SEQUENCE.incrementAndGet() % 1_000_000_000);
    }
}

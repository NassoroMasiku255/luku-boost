package com.lukuboost.electricityloan.electricity.adapter;

import com.lukuboost.electricityloan.electricity.client.ElectricityProviderClient;
import com.lukuboost.electricityloan.electricity.dto.ElectricityTokenPurchase;
import com.lukuboost.electricityloan.electricity.dto.MeterInformation;
import com.lukuboost.electricityloan.electricity.dto.TokenTransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.electricity.provider.active", havingValue = "mock", matchIfMissing = true)
public class MockElectricityProviderAdapter implements ElectricityProviderClient {

    private final Map<String, ElectricityTokenPurchase> purchaseStore = new ConcurrentHashMap<>();

    @Override
    public MeterInformation getMeterInformation(String meterNumber) {
        log.info("MOCK PROVIDER: Getting meter information for meter={}", meterNumber);

        simulateLatency();

        return MeterInformation.builder()
                .meterNumber(meterNumber)
                .customerName("Mock Customer")
                .meterType("PREPAID")
                .meterStatus("ACTIVE")
                .provider("MOCK_PROVIDER")
                .lastToken("1234-5678-9012-3456-7890")
                .lastTokenAmount(new BigDecimal("50000.0000"))
                .lastPurchaseDate(Instant.now().minus(5, ChronoUnit.DAYS))
                .averagePurchaseAmount(new BigDecimal("45000.0000"))
                .purchaseCount(12)
                .tariffClass("DOMESTIC")
                .district("Dar es Salaam")
                .build();
    }

    @Override
    public ElectricityTokenPurchase purchaseElectricity(String meterNumber, BigDecimal amount,
                                                         String transactionReference) {
        log.info("MOCK PROVIDER: Purchasing electricity for meter={}, amount={}, ref={}",
                meterNumber, amount, transactionReference);

        simulateLatency();

        String token = generateMockToken();
        String providerRef = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ElectricityTokenPurchase purchase = ElectricityTokenPurchase.builder()
                .success(true)
                .providerTransactionRef(providerRef)
                .token(token)
                .tokenUnits(amount.divide(new BigDecimal("100")).toPlainString() + " kWh")
                .amount(amount)
                .meterNumber(meterNumber)
                .statusMessage("Token generated successfully")
                .transactionTimestamp(Instant.now())
                .build();

        purchaseStore.put(transactionReference, purchase);

        return purchase;
    }

    @Override
    public TokenTransactionStatus getTransactionStatus(String transactionReference) {
        log.info("MOCK PROVIDER: Checking transaction status for ref={}", transactionReference);

        simulateLatency();

        ElectricityTokenPurchase purchase = purchaseStore.get(transactionReference);
        if (purchase != null) {
            return TokenTransactionStatus.builder()
                    .transactionReference(transactionReference)
                    .status("SUCCESS")
                    .providerTransactionRef(purchase.getProviderTransactionRef())
                    .token(purchase.getToken())
                    .tokenUnits(purchase.getTokenUnits())
                    .statusMessage("Transaction completed successfully")
                    .build();
        }

        return TokenTransactionStatus.builder()
                .transactionReference(transactionReference)
                .status("UNKNOWN")
                .statusMessage("Transaction not found")
                .build();
    }

    private String generateMockToken() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            if (i > 0 && i % 4 == 0) sb.append("-");
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    private void simulateLatency() {
        try {
            Thread.sleep(500 + (long) (Math.random() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

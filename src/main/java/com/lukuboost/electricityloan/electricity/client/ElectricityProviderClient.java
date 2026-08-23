package com.lukuboost.electricityloan.electricity.client;

import com.lukuboost.electricityloan.electricity.dto.ElectricityTokenPurchase;
import com.lukuboost.electricityloan.electricity.dto.MeterInformation;
import com.lukuboost.electricityloan.electricity.dto.TokenTransactionStatus;

import java.math.BigDecimal;

public interface ElectricityProviderClient {

    MeterInformation getMeterInformation(String meterNumber);

    ElectricityTokenPurchase purchaseElectricity(
            String meterNumber,
            BigDecimal amount,
            String transactionReference
    );

    TokenTransactionStatus getTransactionStatus(String transactionReference);
}

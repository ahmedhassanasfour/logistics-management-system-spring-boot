package com.ahmed.logistics.common.idempotency.service;

import com.ahmed.logistics.payment.cod.dto.CollectCodRequest;
import com.ahmed.logistics.payment.dto.CreatePaymentRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class IdempotencyHashService {

    public String hashPaymentCreation(Long shipmentId, CreatePaymentRequest request) {
        String method = (request != null && request.method() != null) ? request.method().name() : "";
        return sha256("CREATE_PAYMENT:" + shipmentId + ":" + method);
    }

    public String hashCodCollection(Long codId, CollectCodRequest request) {
        String amount = "";
        if (request != null && request.collectedAmount() != null) {
            amount = request.collectedAmount().stripTrailingZeros().toPlainString();
        }
        String notes = (request != null && request.notes() != null) ? request.notes().trim() : "";
        return sha256("COLLECT_COD:" + codId + ":" + amount + ":" + notes);
    }

    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in environment", e);
        }
    }
}

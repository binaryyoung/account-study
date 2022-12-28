package com.example.account.dto;

import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryTransactionResponse {
    private String accountNumber;
    private TransactionType transactionType;
    private TransactionResultType transactionResult;
    private String transactionId;
    private Long amount;
    private LocalDateTime transactedAt;

    public static QueryTransactionResponse from(TransactionDto transaction) {
        return QueryTransactionResponse.builder()
            .accountNumber(transaction.getAccountNumber())
            .transactionType(transaction.getTransactionType())
            .transactionResult(transaction.getTransactionResult())
            .transactionId(transaction.getTransactionId())
            .amount(transaction.getAmount())
            .transactedAt(transaction.getTransactedAt())
            .build();
    }
}

package com.example.account.dto;

import com.example.account.domain.Account;
import com.example.account.domain.Transaction;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
    private String accountNumber;
    private TransactionResultType transactionResult;
    private TransactionType transactionType;
    private String transactionId;
    private Long amount;
    private LocalDateTime transactedAt;
    private Long balanceSnapshot;

    public static TransactionDto fromEntity(Transaction transaction) {
        return TransactionDto.builder()
            .accountNumber(transaction.getAccount().getAccountNumber())
            .transactionResult(transaction.getTransactionResultType())
            .transactionType(transaction.getTransactionType())
            .transactionId(transaction.getTransactionId())
            .amount(transaction.getAmount())
            .transactedAt(transaction.getTransactedAt())
            .balanceSnapshot(transaction.getBalanceSnapshot())
            .build();
    }
}

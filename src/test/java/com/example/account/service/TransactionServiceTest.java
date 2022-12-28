package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 성공")
    void successUseBalance() {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.useBalance(1L,
                    "1000000012", 200L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(9800L,captor.getValue().getBalanceSnapshot());

        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 유저 없음")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> transactionService.useBalance(
                1L, "1000000001", 200L)
        );

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 계좌 없음")
    void useBalance_AccountNotFound() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> transactionService.useBalance(
                1L, "1000000001", 200L)
        );

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌 소유주 다름")
    void deleteAccount_userNotMatched() throws Exception {
        //given
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        AccountUser harry = AccountUser.builder()
            .id(13L)
            .name("Harry").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(harry)
                .balance(10000L)
                .accountNumber("1000000001").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(
                1L, "1000000001", 200L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCHED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 거래 금액이 계좌 잔액보다 큼")
    void deleteAccount_amountExceedBalance() throws Exception {
        //given
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(pobi)
                .balance(10000L)
                .accountNumber("1000000001").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(
                12L, "1000000001", 99999L));

        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void successSaveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                .transactionType(USE)
                .transactionResultType(F)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransaction("1000000012", 200L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(10000L,captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResultType());
    }

    @Test
    @DisplayName("잔액 사용 취소 성공")
    void successCancelBalance() {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(9000L)
            .accountNumber("1000000012").build();
        Transaction transaction = Transaction.builder()
            .transactionType(USE)
            .transactionResultType(S)
            .account(account)
            .amount(1000L)
            .balanceSnapshot(10000L)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now())
            .build();
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
        given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                .transactionType(CANCEL)
                .transactionResultType(S)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(10000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId",
            "1000000012", 1000L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(10000L,captor.getValue().getBalanceSnapshot());

        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 거래 없음")
    void cancelBalance_UserNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance(
                "transactionId", "1000000012", 1000L)
        );

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 요청한 계좌번호와 거래에 사용된 계좌번호 불일치")
    void cancelBalance_AccountNotFound() throws Exception {
        //given
        given(transactionRepository.findByTransactionId("transactionId"))
            .willReturn(Optional.of(Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .amount(1000L)
                .balanceSnapshot(10000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build()));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance(
                "transactionId", "1000000001", 200L)
        );

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래와 계좌가 매칭실패")
    void cancelBalance_TransactionAccountUnMatch() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        Account account = Account.builder()
            .id(1L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(9000L)
            .accountNumber("1000000012").build();
        Account accountNotUse = Account.builder()
            .id(2L)
            .accountStatus(IN_USE)
            .balance(9000L)
            .accountNumber("1000000013").build();
        Transaction transaction = Transaction.builder()
            .transactionType(USE)
            .transactionResultType(S)
            .account(account)
            .amount(1000L)
            .balanceSnapshot(10000L)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now())
            .build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(accountNotUse));

        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance(
                "transactionId", "1000000013", 1000L)
        );

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UNMATCHED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래금액과 취소금액이 다름")
    void cancelBalance_CancelMustFully() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        Account account = Account.builder()
            .id(1L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(9000L)
            .accountNumber("1000000012").build();
        Transaction transaction = Transaction.builder()
            .transactionType(USE)
            .transactionResultType(S)
            .account(account)
            .amount(1000L)
            .balanceSnapshot(10000L)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now())
            .build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance(
                "transactionId", "1000000012", 100L)
        );

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 1년이상 지난 거래 취소 불가")
    void cancelBalance_TooOldOrderToCancel() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        Account account = Account.builder()
            .id(1L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(9000L)
            .accountNumber("1000000012").build();
        Transaction transaction = Transaction.builder()
            .transactionType(USE)
            .transactionResultType(S)
            .account(account)
            .amount(1000L)
            .balanceSnapshot(10000L)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
            .build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> transactionService.cancelBalance(
                "transactionId", "1000000012", 1000L)
        );

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, accountException.getErrorCode());
    }

    @Test
    void successQueryTransaction() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        Account account = Account.builder()
            .id(1L)
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(9000L)
            .accountNumber("1000000012").build();
        Transaction transaction = Transaction.builder()
            .transactionType(USE)
            .transactionResultType(S)
            .account(account)
            .amount(1000L)
            .balanceSnapshot(10000L)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
            .build();
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));

        //when
        TransactionDto transactionDto = transactionService.queryTransaction("transactionId");

        //then
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(1000L, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("거래 조회 실패 - 원거래 없음")
    void queryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
            () -> transactionService.queryTransaction("transactionId")
        );

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }
}
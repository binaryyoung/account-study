package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.AccountRepository;
import com.example.account.type.AccountStatus;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
            .willReturn(Optional.of(Account.builder()
                .accountNumber("1000000012").build()));
        given(accountRepository.save(any()))
            .willReturn(Account.builder()
                .accountUser(user)
                .accountNumber("1000000013").build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    void createFirstAccount() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(15L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
            .willReturn(Optional.empty());
        given(accountRepository.save(any()))
            .willReturn(Account.builder()
                .accountUser(user)
                .accountNumber("1000000013").build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_UserNotFound() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저 당 최대 계좌는 10개")
    void createAccount_maxAccountIs10() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(15L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
            .willReturn(10);

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    void deleteAccountSuccess() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(user)
                .balance(0L)
                .accountNumber("1000000001").build()));
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(12L, "1000000001");

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000001", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccount_UserNotFound() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1000000001"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 없음 - 계좌 해지 실패")
    void deleteAccount_AccountNotFound() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(AccountUser.builder().id(1L).build()));

        given(accountRepository.findByAccountNumber("1000000001"))
            .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1000000001"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름")
    void deleteAccount_userNotMatched() throws Exception {
        //given
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        AccountUser harry = AccountUser.builder()
            .id(12L)
            .name("Harry").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(harry)
                .balance(0L)
                .accountNumber("1000000001").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1000000001"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCHED, exception.getErrorCode());
    }


    @Test
    @DisplayName("해지 계좌는 잔액이 없어야 한다")
    void deleteAccount_balanceNotEmpty() throws Exception {
        //given
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(pobi)
                .balance(100L)
                .accountNumber("1000000001").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1000000001"));

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 해지할 수 없다")
    void deleteAccount_accountAlreadyUnregistered() throws Exception {
        //given
        AccountUser pobi = AccountUser.builder()
            .id(12L)
            .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                .accountUser(pobi)
                .balance(0L)
                .accountStatus(AccountStatus.UNREGISTERED)
                .accountNumber("1000000001").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
            () -> accountService.deleteAccount(1L, "1000000001"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

}
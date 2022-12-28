package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     * 사용자가 있는지 조회
     * - optinal 반환
     * - custom runtime exception + ErrorCode 클래스
     * 계좌의 번호를 생성하고
     * - max id 조회 + 문자열 변환 + 기본값 처리
     * 계좌를 저장하고, 그 정보를 넘긴다.
     * - enum type import static 처리해서 가독성 좋게
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        validateCreateAccount(accountUser);

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
            .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
            .orElse("1000000000");

        return AccountDto.fromEntity(accountRepository.save(
            Account.builder()
                .accountUser(accountUser)
                .accountStatus(IN_USE)
                .accountNumber(newAccountNumber)
                .balance(initialBalance)
                .registeredAt(LocalDateTime.now())
                .build()
        ));
    }

    /**
     * service 에 validation 별도로 빼는 게 가독성 좋음.
     */
    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.unRegister();

        // 없어도 되는 코드인데 테스트 원활하게 하기 위해 코드 생성
        accountRepository.save(account);

        return AccountDto.builder()
            .userId(accountUser.getId())
            .accountNumber(account.getAccountNumber())
            .unRegisteredAt(account.getUnRegisteredAt()).build();
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser, account.getAccountUser())) {
            throw new AccountException(USER_ACCOUNT_UNMATCHED);
        }

        if (AccountStatus.UNREGISTERED.equals(account.getAccountStatus())) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        return accounts.stream()
            .filter(account -> IN_USE.equals(account.getAccountStatus()))
            .map(AccountDto::fromEntity)
            .collect(Collectors.toList());
    }
}

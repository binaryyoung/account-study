package com.example.account.domain;

import com.example.account.exception.AccountException;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

import static com.example.account.type.ErrorCode.SYSTEM_MAX_ACCOUNT;

@Getter
@Setter
@NoArgsConstructor // jpa 용 기본생성자
@AllArgsConstructor // 테스트 용
@Builder // 테스트 용
@Entity
@SequenceGenerator(
    name = "ACCOUNT_NUMBER_SEQ_GENERATOR"
    , sequenceName = "ACCOUNT_NUMBER_SEQ"
    , initialValue = 1000000000
    , allocationSize = 1
)
public class Account extends BaseEntity{
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private AccountUser accountUser;

    @Column(unique=true)
    @GeneratedValue(generator = "ACCOUNT_NUMBER_SEQ")
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;
    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    public void unRegister() {
        this.accountStatus = AccountStatus.UNREGISTERED;
        this.unRegisteredAt = LocalDateTime.now();
    }

    public void useBalance(Long amount) {
        if (amount > balance) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
        balance -= amount;
    }

    public void cancelBalance(Long amount) {
        if (amount < 0) {
            throw new AccountException(ErrorCode.INVALID_REQUEST);
        }
        balance += amount;
    }

    public void initAccountNumber() {
        if (id > 9_999_999_999L) {
            throw new AccountException(SYSTEM_MAX_ACCOUNT);
        }
        accountNumber = String.format("%010d", id);
    }
}

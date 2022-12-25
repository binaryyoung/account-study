package com.example.account.repository;

import com.example.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // spring data jpa 에서 제공하는 JpaRepository<entityClass, pkType>
public interface AccountRepository extends JpaRepository<Account, Long> {

}

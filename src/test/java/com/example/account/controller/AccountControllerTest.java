package com.example.account.controller;

import com.example.account.domain.Account;
import com.example.account.domain.AccountStatus;
import com.example.account.service.AccountService;
import com.example.account.service.RedisTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @MockBean
    private AccountService accountService;

    @MockBean
    private RedisTestService redisTestService;

    @Autowired
    private MockMvc mockMvc;

    @Test
//    @DisplayName("")
    void successGetAccount() throws Exception {
        //given
        given(accountService.getAccount(anyLong()))
            .willReturn(Account.builder()
                .accountNumber("3456")
                .accountStatus(AccountStatus.IN_USE)
                .build());

        //when
        //then
        mockMvc.perform(get("/account/567")) // 괄호 2개네
            .andDo(print())
            .andExpect(jsonPath("$.accountNumber").value("3456"))
            .andExpect(jsonPath("$.accountStatus").value("IN_USE"))
            .andExpect(status().isOk());
    }
}
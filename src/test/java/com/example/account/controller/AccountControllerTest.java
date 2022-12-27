package com.example.account.controller;

import com.example.account.dto.AccountDto;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.service.AccountService;
import com.example.account.service.RedisTestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successCreateAccount() throws Exception {
        //given
        given(accountService.createAccount(anyLong(), anyLong()))
            .willReturn(AccountDto.builder()
                .userId(1L)
                .accountNumber("1234567890")
                .registeredAt(LocalDateTime.now())
                .build());

        //when
        //then
        mockMvc.perform(post("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateAccount.Request(3333L, 1111L)
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.accountNumber").value("1234567890"))
            .andDo(print());
    }

    @Test
    void successDeleteAccount() throws Exception {
        //given
        long userId = 1L;
        String accountNumber = "1234567890";
        LocalDateTime unRegisteredAt = LocalDateTime.now();
        given(accountService.deleteAccount(anyLong(), anyString()))
            .willReturn(AccountDto.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .unRegisteredAt(unRegisteredAt)
                .build());

        //when
        //then
        mockMvc.perform(delete("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new DeleteAccount.Request(userId, accountNumber)
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.accountNumber").value(accountNumber))
            .andExpect(jsonPath("$.unRegisteredAt").value(unRegisteredAt.toString()))
            .andDo(print());
    }

    @Test
    void successGetAccountsByUserId() throws Exception {
        //given
        List<AccountDto> accountDtos = List.of(
            AccountDto.builder().accountNumber("1234567890").balance(1000L).build(),
            AccountDto.builder().accountNumber("1234567891").balance(2000L).build(),
            AccountDto.builder().accountNumber("1234567893").balance(3000L).build()
        );
        given(accountService.getAccountsByUserId(anyLong()))
            .willReturn(accountDtos);

        //when
        //then
        mockMvc.perform(get("/account?user_id=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].accountNumber").value("1234567890"))
            .andExpect(jsonPath("$[0].balance").value(1000))
            .andExpect(jsonPath("$[1].accountNumber").value("1234567891"))
            .andExpect(jsonPath("$[1].balance").value(2000))
            .andExpect(jsonPath("$[2].accountNumber").value("1234567893"))
            .andExpect(jsonPath("$[2].balance").value(3000))
            .andDo(print());
    }
}
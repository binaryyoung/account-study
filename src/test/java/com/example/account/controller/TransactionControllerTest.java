package com.example.account.controller;

import com.example.account.dto.CancelBalance;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successUseBalance() throws Exception {
        //given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
            .willReturn(TransactionDto.builder()
                .accountNumber("1000000000")
                .transactedAt(LocalDateTime.now())
                .amount(12345L)
                .transactionId("transactionId")
                .transactionResult(S)
                .build());

        //when
        //then
        mockMvc.perform(post("/transaction/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UseBalance.Request(1L, "1000000000", 12345L)
                )))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("1000000000"))
            .andExpect(jsonPath("$.transactionResult").value("S"))
            .andExpect(jsonPath("$.transactionId").value("transactionId"))
            .andExpect(jsonPath("$.amount").value(12345));
    }

    @Test
    void successCancelBalance() throws Exception {
        //given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
            .willReturn(TransactionDto.builder()
                .accountNumber("1000000000")
                .transactedAt(LocalDateTime.now())
                .amount(54321L)
                .transactionId("transactionIdForCancel")
                .transactionResult(S)
                .build());

        //when
        //then
        mockMvc.perform(post("/transaction/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CancelBalance.Request(
                        "transactionIdForCancel", "1000000000", 54321L)
                )))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("1000000000"))
            .andExpect(jsonPath("$.transactionResult").value("S"))
            .andExpect(jsonPath("$.transactionId").value("transactionIdForCancel"))
            .andExpect(jsonPath("$.amount").value(54321));
    }

    @Test
    void successQueryTransaction() throws Exception {
        //given
        TransactionDto transactionDto = TransactionDto.builder()
            .accountNumber("1000000000")
            .transactedAt(LocalDateTime.now())
            .amount(12345L)
            .transactionId("testId")
            .transactionResult(S)
            .transactionType(CANCEL)
            .build();
        given(transactionService.queryTransaction(anyString()))
            .willReturn(transactionDto);

        //when
        //then
        mockMvc.perform(get("/transaction?transactionId=testId"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value(transactionDto.getAccountNumber()))
            .andExpect(jsonPath("$.transactionType").value(transactionDto.getTransactionType().toString()))
            .andExpect(jsonPath("$.transactionResult").value(transactionDto.getTransactionResult().toString()))
            .andExpect(jsonPath("$.transactionId").value(transactionDto.getTransactionId()))
            .andExpect(jsonPath("$.amount").value(transactionDto.getAmount()))
            .andExpect(jsonPath("$.transactedAt").value(transactionDto.getTransactedAt().toString()))
            .andDo(print());
    }
}
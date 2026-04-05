package com.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.dto.CardDTO;
import com.security.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardController Tests")
class CardControllerTest {

    @Mock private CardService cardService;
    @InjectMocks private CardController cardController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cardController).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /api/card")
    class CriarTests {

        @Test
        @DisplayName("deve criar cartão com sucesso")
        void shouldCreateCard() throws Exception {
            CardDTO cardDTO = new CardDTO();
            cardDTO.setNumero("4111111111111111");
            cardDTO.setNome("Jose Silva");
            cardDTO.setCodigoSeguranca("123");
            cardDTO.setDataValidade("12/28");
            CardDTO response = new CardDTO();
            response.setId(1L);
            response.setNumero("4111111111111111");
            response.setNome("Jose Silva");

            when(cardService.criar(any(CardDTO.class))).thenReturn(response);

            mockMvc.perform(post("/api/card")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("DELETE /api/card/{id}")
    class EliminarTests {

        @Test
        @DisplayName("deve eliminar cartão por ID")
        void shouldDeleteCard() throws Exception {
            mockMvc.perform(delete("/api/card/1"))
                    .andExpect(status().isCreated());
        }
    }
}

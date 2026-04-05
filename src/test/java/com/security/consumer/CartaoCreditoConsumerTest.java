package com.security.consumer;

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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartaoCreditoConsumer Tests")
class CartaoCreditoConsumerTest {

    @Mock private CardService cardService;
    @InjectMocks private CartaoCreditoConsumer consumer;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("consumir")
    class ConsumirTests {

        @Test
        @DisplayName("deve processar mensagem valida e salvar cartao")
        void shouldProcessValidMessage() throws Exception {
            CardDTO cardDTO = new CardDTO();
            cardDTO.setNumero("4111111111111111");
            cardDTO.setNome("Jose Silva");
            cardDTO.setCodigoSeguranca("123");
            cardDTO.setDataValidade("12/28");

            String mensagem = objectMapper.writeValueAsString(cardDTO);

            CardDTO saved = new CardDTO();
            saved.setId(1L);
            when(cardService.criar(any(CardDTO.class))).thenReturn(saved);

            consumer.consumir(mensagem, "cartao_credito", 0, 0L);

            verify(cardService).criar(any(CardDTO.class));
        }

        @Test
        @DisplayName("deve lancar RuntimeException para mensagem invalida")
        void shouldThrowOnInvalidMessage() {
            String mensagemInvalida = "{ json invalido }}}";

            assertThatThrownBy(() -> consumer.consumir(mensagemInvalida, "cartao_credito", 0, 0L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Falha ao processar mensagem");

            verifyNoInteractions(cardService);
        }
    }
}

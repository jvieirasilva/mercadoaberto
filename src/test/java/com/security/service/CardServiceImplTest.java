package com.security.service;

import com.security.dto.CardDTO;
import com.security.model.Card;
import com.security.repository.CardRepository;
import com.security.service.impl.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardServiceImpl Tests")
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private CardDTO cardDTO;
    private Card cardSalvo;

    @BeforeEach
    void setUp() {
        cardDTO = CardDTO.builder()
                .numero("1234567890123456")
                .dataValidade("12/2028")
                .codigoSeguranca("123")
                .nome("José Silva")
                .build();

        cardSalvo = Card.builder()
                .id(1L)
                .numero("1234567890123456")
                .dataValidade("12/2028")
                .codigoSeguranca("123")
                .nome("José Silva")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // criar()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("criar()")
    class CriarTests {

        @Test
        @DisplayName("Deve criar cartão com sucesso e retornar DTO preenchido")
        void shouldCreateCardSuccessfully() {
            when(cardRepository.save(any(Card.class))).thenReturn(cardSalvo);

            CardDTO resultado = cardService.criar(cardDTO);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(1L);
            assertThat(resultado.getNumero()).isEqualTo("1234567890123456");
            assertThat(resultado.getDataValidade()).isEqualTo("12/2028");
            assertThat(resultado.getCodigoSeguranca()).isEqualTo("123");
            assertThat(resultado.getNome()).isEqualTo("José Silva");
        }

        @Test
        @DisplayName("Deve mapear corretamente os campos do DTO para a entidade")
        void shouldMapDtoFieldsToEntity() {
            ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
            when(cardRepository.save(captor.capture())).thenReturn(cardSalvo);

            cardService.criar(cardDTO);

            Card cardCapturado = captor.getValue();
            assertThat(cardCapturado.getNumero()).isEqualTo("1234567890123456");
            assertThat(cardCapturado.getDataValidade()).isEqualTo("12/2028");
            assertThat(cardCapturado.getCodigoSeguranca()).isEqualTo("123");
            assertThat(cardCapturado.getNome()).isEqualTo("José Silva");
        }

        @Test
        @DisplayName("Deve chamar o repositório save exatamente uma vez")
        void shouldCallRepositorySaveOnce() {
            when(cardRepository.save(any(Card.class))).thenReturn(cardSalvo);

            cardService.criar(cardDTO);

            verify(cardRepository, times(1)).save(any(Card.class));
        }

        @Test
        @DisplayName("Deve criar cartão apenas com número (campos opcionais nulos)")
        void shouldCreateCardWithOnlyRequiredFields() {
            CardDTO dtoMinimo = CardDTO.builder()
                    .numero("9876543210987654")
                    .build();

            Card cardMinimo = Card.builder()
                    .id(2L)
                    .numero("9876543210987654")
                    .build();

            when(cardRepository.save(any(Card.class))).thenReturn(cardMinimo);

            CardDTO resultado = cardService.criar(dtoMinimo);

            assertThat(resultado.getNumero()).isEqualTo("9876543210987654");
            assertThat(resultado.getDataValidade()).isNull();
            assertThat(resultado.getCodigoSeguranca()).isNull();
            assertThat(resultado.getNome()).isNull();
        }

        @Test
        @DisplayName("Deve retornar o ID gerado pelo repositório")
        void shouldReturnGeneratedId() {
            Card cardComId = Card.builder()
                    .id(99L)
                    .numero("1111222233334444")
                    .build();

            when(cardRepository.save(any(Card.class))).thenReturn(cardComId);

            CardDTO resultado = cardService.criar(CardDTO.builder()
                    .numero("1111222233334444")
                    .build());

            assertThat(resultado.getId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("Deve propagar exceção quando o repositório falha")
        void shouldPropagateExceptionWhenRepositoryFails() {
            when(cardRepository.save(any(Card.class)))
                    .thenThrow(new RuntimeException("Erro de base de dados"));

            assertThatThrownBy(() -> cardService.criar(cardDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro de base de dados");
        }

        @Test
        @DisplayName("Deve preservar o ID do DTO quando fornecido")
        void shouldPreserveIdFromDto() {
            cardDTO.setId(10L);

            Card cardComId = Card.builder()
                    .id(10L)
                    .numero("1234567890123456")
                    .dataValidade("12/2028")
                    .codigoSeguranca("123")
                    .nome("José Silva")
                    .build();

            ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
            when(cardRepository.save(captor.capture())).thenReturn(cardComId);

            CardDTO resultado = cardService.criar(cardDTO);

            assertThat(captor.getValue().getId()).isEqualTo(10L);
            assertThat(resultado.getId()).isEqualTo(10L);
        }
    }
}

package com.security.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.dto.CardDTO;
import com.security.service.CardService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CartaoCreditoConsumer {

    private static final Logger log = LoggerFactory.getLogger(CartaoCreditoConsumer.class);
    private static final String TOPIC = "cartao_credito";
    private static final String TOPIC_DELETE = "delete-cartao";
    private static final String GROUP_ID = "cartao-credito-group-v3"; // ← novo group-id

    private final CardService cardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID) // ← sem containerFactory
    public void consumir(
            @Payload String mensagem,              // ← String, não CardDTO
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("✅ Mensagem recebida — topic: {}, partition: {}, offset: {}", topic, partition, offset);

        try {
            CardDTO cardDTO = objectMapper.readValue(mensagem, CardDTO.class);
            log.info("📦 CardDTO: numero={}, nome={}", cardDTO.getNumero(), cardDTO.getNome());

            CardDTO resultado = cardService.criar(cardDTO);
            log.info("✅ Cartão salvo com ID: {}", resultado.getId());

        } catch (Exception e) {
            log.error("❌ Erro: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao processar mensagem", e);
        }
    }
 
    
    
    
}
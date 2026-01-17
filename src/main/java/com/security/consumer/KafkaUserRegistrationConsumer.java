package com.security.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

//@Component
//@RequiredArgsConstructor
public class KafkaUserRegistrationConsumer {

 /*   private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "user-registration", groupId = "user-registration-consumer")
    public void consumeMessage(String message) {
        try {
            RegisterRequest request = objectMapper.readValue(message, RegisterRequest.class);

            restTemplate.postForEntity(
                    "http://localhost:8083/api/auth/register",
                    request,
                    String.class
            );

            System.out.println("✅ Usuário enviado para /api/auth/register com sucesso!");

        } catch (Exception e) {
            System.err.println("❌ Erro ao processar mensagem Kafka: " + e.getMessage());
        }
    }
    */
}
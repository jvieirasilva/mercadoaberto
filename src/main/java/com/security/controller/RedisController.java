package com.security.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:4200", "http://localhost:3000", "http://192.168.49.2:30999"}, allowCredentials = "true")
@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class RedisController {

    private final StringRedisTemplate redisTemplate;
    private static final Logger log = LoggerFactory.getLogger(RedisController.class);

    @PostMapping("/cache")
    public ResponseEntity<Void> setCacheValue(@RequestParam String key, @RequestParam String value) {
        log.info("Salvando no Redis - Chave: {}, Valor: {}", key, value);
        redisTemplate.opsForValue().set(key, value);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/cache")
    public ResponseEntity<String> getCache(@RequestParam String key) {
        String value = redisTemplate.opsForValue().get(key);
        return ResponseEntity.ok(value);
    }
    
}

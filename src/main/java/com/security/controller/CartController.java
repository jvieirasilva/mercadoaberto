package com.security.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.security.dto.AddToCartRequest;
import com.security.dto.CartResponse;
import com.security.dto.UpdateCartItemRequest;
import com.security.service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    
    private final CartService cartService;
    
    /**
     * ✅ Adicionar produto ao carrinho
     * POST /api/cart/add
     */
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        log.info("POST /api/cart/add - User: {}, Product: {}", userEmail, request.getProductId());
        
        CartResponse response = cartService.addToCart(userEmail, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * ✅ Buscar carrinho do usuário logado
     * GET /api/cart
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("GET /api/cart - User: {}", userEmail);
        
        CartResponse response = cartService.getCart(userEmail);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ✅ Atualizar quantidade de item no carrinho
     * PUT /api/cart/items/{cartItemId}
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        log.info("PUT /api/cart/items/{} - User: {}, New Quantity: {}", 
                cartItemId, userEmail, request.getQuantity());
        
        CartResponse response = cartService.updateCartItem(userEmail, cartItemId, request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ✅ Remover item do carrinho
     * DELETE /api/cart/items/{cartItemId}
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        log.info("DELETE /api/cart/items/{} - User: {}", cartItemId, userEmail);
        
        CartResponse response = cartService.removeCartItem(userEmail, cartItemId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ✅ Limpar carrinho (remover todos os itens)
     * DELETE /api/cart/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("DELETE /api/cart/clear - User: {}", userEmail);
        
        cartService.clearCart(userEmail);
        
        return ResponseEntity.noContent().build();
    }
}
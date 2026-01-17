package com.security.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.security.dto.AddToCartRequest;
import com.security.dto.CartResponse;
import com.security.dto.CartItemResponse;
import com.security.dto.UpdateCartItemRequest;
import com.security.exception.ResourceNotFoundException;
import com.security.model.Cart;
import com.security.model.CartItem;
import com.security.model.Product;
import com.security.model.User;
import com.security.repository.CartItemRepository;
import com.security.repository.CartRepository;
import com.security.repository.ProductRepository;
import com.security.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    // ✅ Adicionar produto ao carrinho
    @Transactional
    public CartResponse addToCart(String userEmail, AddToCartRequest request) {
        log.info("Adding product {} to cart for user {}", request.getProductId(), userEmail);
        
        // Buscar ou criar carrinho
        Cart cart = getOrCreateCart(userEmail);
        
        // Buscar produto
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
        
        // ✅ CORRIGIDO: Verificar estoque - Comparar BigDecimal corretamente
        BigDecimal requestedQuantity = BigDecimal.valueOf(request.getQuantity());
        if (product.getStockQuantity().compareTo(requestedQuantity) < 0) {
            throw new IllegalStateException("Insufficient stock. Available: " + product.getStockQuantity());
        }
        
        // Verificar se produto já está no carrinho
        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);
        
        if (existingItem != null) {
            // ✅ CORRIGIDO: Atualizar quantidade - Converter para BigDecimal
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            BigDecimal newQuantityBD = BigDecimal.valueOf(newQuantity);
            
            if (product.getStockQuantity().compareTo(newQuantityBD) < 0) {
                throw new IllegalStateException("Quantity exceeds available stock");
            }
            
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            
            log.info("Updated quantity for product {} in cart", product.getId());
        } else {
            // Criar novo item
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .priceAtAddition(product.getPrice())
                    .build();
            
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
            
            log.info("Added new product {} to cart", product.getId());
        }
        
        cart = cartRepository.save(cart);
        
        return mapToCartResponse(cart);
    }
    
    // ✅ Buscar carrinho do usuário
    @Transactional(readOnly = true)
    public CartResponse getCart(String userEmail) {
        log.info("Fetching cart for user {}", userEmail);
        
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    log.info("No cart found for user {}, creating empty response", userEmail);
                    User user = userRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    
                    return Cart.builder()
                            .user(user)
                            .build();
                });
        
        return mapToCartResponse(cart);
    }
    
    // ✅ Atualizar quantidade de item
    @Transactional
    public CartResponse updateCartItem(String userEmail, Long cartItemId, UpdateCartItemRequest request) {
        log.info("Updating cart item {} quantity to {}", cartItemId, request.getQuantity());
        
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
        
        // Verificar se item pertence ao carrinho do usuário
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalStateException("Cart item does not belong to user's cart");
        }
        
        // ✅ CORRIGIDO: Verificar estoque - Comparar BigDecimal corretamente
        Product product = cartItem.getProduct();
        BigDecimal requestedQuantity = BigDecimal.valueOf(request.getQuantity());
        
        if (requestedQuantity.compareTo(product.getStockQuantity()) > 0) {
            throw new IllegalStateException("Quantity exceeds available stock: " + product.getStockQuantity());
        }
        
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        
        cart = cartRepository.save(cart);
        
        log.info("Cart item {} updated successfully", cartItemId);
        
        return mapToCartResponse(cart);
    }
    
    // ✅ Remover item do carrinho
    @Transactional
    public CartResponse removeCartItem(String userEmail, Long cartItemId) {
        log.info("Removing cart item {} for user {}", cartItemId, userEmail);
        
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
        
        // Verificar se item pertence ao carrinho do usuário
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalStateException("Cart item does not belong to user's cart");
        }
        
        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);
        
        cart = cartRepository.save(cart);
        
        log.info("Cart item {} removed successfully", cartItemId);
        
        return mapToCartResponse(cart);
    }
    
    // ✅ Limpar carrinho
    @Transactional
    public void clearCart(String userEmail) {
        log.info("Clearing cart for user {}", userEmail);
        
        Cart cart = cartRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cartRepository.save(cart);
        
        log.info("Cart cleared successfully for user {}", userEmail);
    }
    
    // ✅ Buscar ou criar carrinho
    private Cart getOrCreateCart(String userEmail) {
        return cartRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
                    
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    
                    log.info("Creating new cart for user {}", userEmail);
                    return cartRepository.save(newCart);
                });
    }
    
    // ✅ Mapear Cart para CartResponse
    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());
        
        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUser().getId())
                .userEmail(cart.getUser().getEmail())
                .items(itemResponses)
                .totalItems(cart.getTotalItems())
                .totalPrice(cart.getTotalPrice())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
    
    // ✅ Mapear CartItem para CartItemResponse
    private CartItemResponse mapToCartItemResponse(CartItem item) {
        Product product = item.getProduct();
        
        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productDescription(product.getDescription())
                // ✅ CORRIGIDO: Usar profileImageUrl se imageUrl não existir
                // Ajuste conforme o nome do campo no seu Product
                .productImageUrl(getProductImageUrl(product))
                .quantity(item.getQuantity())
                .priceAtAddition(item.getPriceAtAddition())
                .subtotal(item.getSubtotal())
                .maxStock(product.getStockQuantity())
                .build();
    }
    
    private String getProductImageUrl(Product product) {
        // ✅ CORRETO: Pegar primeira imagem da lista
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0);
        }
        
        log.warn("Product {} does not have images", product.getId());
        return null;
    }
    
}
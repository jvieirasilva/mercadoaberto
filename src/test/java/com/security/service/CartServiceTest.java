package com.security.service;

import com.security.dto.AddToCartRequest;
import com.security.dto.CartResponse;
import com.security.dto.UpdateCartItemRequest;
import com.security.exception.ResourceNotFoundException;
import com.security.model.Cart;
import com.security.model.CartItem;
import com.security.model.Product;
import com.security.model.User;
import com.security.model.Role;
import com.security.repository.CartItemRepository;
import com.security.repository.CartRepository;
import com.security.repository.ProductRepository;
import com.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("jose@test.com")
                .fullName("José")
                .role(Role.USER)
                .isActive(true)
                .isNotLocked(true)
                .build();

        product = new Product();
        product.setId(10L);
        product.setName("Produto Teste");
        product.setPrice(new BigDecimal("29.99"));
        product.setStockQuantity(new BigDecimal("100.00"));
        product.setDescription("Descrição do produto");
        product.setIsActive(true);
        product.setImages(List.of("https://s3.amazonaws.com/img.png"));

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        cartItem = CartItem.builder()
                .id(100L)
                .cart(cart)
                .product(product)
                .quantity(2)
                .priceAtAddition(new BigDecimal("29.99"))
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // addToCart()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addToCart()")
    class AddToCartTests {

        @Test
        @DisplayName("Deve adicionar novo produto ao carrinho com sucesso")
        void shouldAddNewProductToCart() {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(10L)
                    .quantity(2)
                    .build();

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            CartResponse response = cartService.addToCart("jose@test.com", request);

            assertThat(response).isNotNull();
            assertThat(response.getUserEmail()).isEqualTo("jose@test.com");
            assertThat(response.getCartId()).isEqualTo(1L);

            verify(cartItemRepository).save(argThat(item ->
                    item.getQuantity() == 2
                    && item.getPriceAtAddition().compareTo(new BigDecimal("29.99")) == 0
            ));
        }

        @Test
        @DisplayName("Deve atualizar quantidade quando produto já está no carrinho")
        void shouldUpdateQuantityWhenProductAlreadyInCart() {
            cart.getItems().add(cartItem); // já tem 2 unidades

            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(10L)
                    .quantity(3)
                    .build();

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartIdAndProductId(1L, 10L))
                    .thenReturn(Optional.of(cartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            cartService.addToCart("jose@test.com", request);

            // 2 (existente) + 3 (novo) = 5
            verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 5));
        }

        @Test
        @DisplayName("Deve criar novo carrinho quando usuário não tem carrinho")
        void shouldCreateNewCartWhenUserHasNone() {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(10L)
                    .quantity(1)
                    .build();

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("jose@test.com")).thenReturn(Optional.of(user));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartIdAndProductId(any(), any())).thenReturn(Optional.empty());
            when(cartItemRepository.save(any())).thenReturn(cartItem);

            CartResponse response = cartService.addToCart("jose@test.com", request);

            assertThat(response).isNotNull();
            verify(cartRepository, atLeastOnce()).save(any(Cart.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(999L)
                    .quantity(1)
                    .build();

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart("jose@test.com", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Deve lançar exceção quando estoque é insuficiente")
        void shouldThrowWhenInsufficientStock() {
            product.setStockQuantity(new BigDecimal("1.00")); // apenas 1 em estoque

            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(10L)
                    .quantity(5) // mais que o estoque
                    .build();

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> cartService.addToCart("jose@test.com", request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        @DisplayName("Deve lançar exceção quando quantidade total excede estoque ao atualizar")
        void shouldThrowWhenTotalQuantityExceedsStock() {
            product.setStockQuantity(new BigDecimal("3.00"));
            cartItem.setQuantity(2); // já tem 2 no carrinho
            cart.getItems().add(cartItem);

            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(10L)
                    .quantity(2) // 2 + 2 = 4 > estoque(3)
                    .build();

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(productRepository.findById(10L)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCartIdAndProductId(1L, 10L))
                    .thenReturn(Optional.of(cartItem));

            assertThatThrownBy(() -> cartService.addToCart("jose@test.com", request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("exceeds");
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não existe ao criar carrinho")
        void shouldThrowWhenUserNotFoundOnCartCreation() {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(10L)
                    .quantity(1)
                    .build();

            when(cartRepository.findByUserEmail("unknown@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart("unknown@test.com", request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getCart()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getCart()")
    class GetCartTests {

        @Test
        @DisplayName("Deve retornar carrinho do usuário com itens")
        void shouldReturnCartWithItems() {
            cart.getItems().add(cartItem);
            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));

            CartResponse response = cartService.getCart("jose@test.com");

            assertThat(response).isNotNull();
            assertThat(response.getCartId()).isEqualTo(1L);
            assertThat(response.getUserEmail()).isEqualTo("jose@test.com");
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getTotalItems()).isEqualTo(2); // quantity=2
            assertThat(response.getTotalPrice()).isEqualByComparingTo("59.98"); // 29.99 * 2
        }

        @Test
        @DisplayName("Deve retornar carrinho vazio quando usuário não tem carrinho ainda")
        void shouldReturnEmptyCartWhenNoneExists() {
            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("jose@test.com")).thenReturn(Optional.of(user));

            CartResponse response = cartService.getCart("jose@test.com");

            assertThat(response).isNotNull();
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotalItems()).isZero();
        }

        @Test
        @DisplayName("Deve retornar carrinho com totalPrice correto para múltiplos itens")
        void shouldCalculateTotalPriceCorrectly() {
            Product product2 = new Product();
            product2.setId(20L);
            product2.setName("Produto 2");
            product2.setPrice(new BigDecimal("10.00"));
            product2.setStockQuantity(new BigDecimal("50.00"));
            product2.setImages(new ArrayList<>());

            CartItem item2 = CartItem.builder()
                    .id(101L)
                    .cart(cart)
                    .product(product2)
                    .quantity(3)
                    .priceAtAddition(new BigDecimal("10.00"))
                    .build();

            cart.getItems().add(cartItem);  // 2 x 29.99 = 59.98
            cart.getItems().add(item2);     // 3 x 10.00 = 30.00

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));

            CartResponse response = cartService.getCart("jose@test.com");

            assertThat(response.getTotalPrice()).isEqualByComparingTo("89.98");
            assertThat(response.getTotalItems()).isEqualTo(5); // 2 + 3
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateCartItem()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateCartItem()")
    class UpdateCartItemTests {

        @Test
        @DisplayName("Deve atualizar quantidade do item com sucesso")
        void shouldUpdateCartItemQuantity() {
            cart.getItems().add(cartItem);
            UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(5).build();

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(cartItemRepository.findById(100L)).thenReturn(Optional.of(cartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            CartResponse response = cartService.updateCartItem("jose@test.com", 100L, request);

            verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 5));
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Deve lançar exceção quando item não pertence ao carrinho do usuário")
        void shouldThrowWhenItemNotInUserCart() {
            Cart otherCart = Cart.builder().id(99L).user(user).items(new ArrayList<>()).build();
            cartItem.setCart(otherCart); // item pertence a outro carrinho

            UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(5).build();

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(cartItemRepository.findById(100L)).thenReturn(Optional.of(cartItem));

            assertThatThrownBy(() -> cartService.updateCartItem("jose@test.com", 100L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("Deve lançar exceção quando quantidade excede estoque")
        void shouldThrowWhenQuantityExceedsStock() {
            product.setStockQuantity(new BigDecimal("3.00"));
            cart.getItems().add(cartItem);
            UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(10).build();

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(cartItemRepository.findById(100L)).thenReturn(Optional.of(cartItem));

            assertThatThrownBy(() -> cartService.updateCartItem("jose@test.com", 100L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("exceeds available stock");
        }

        @Test
        @DisplayName("Deve lançar exceção quando carrinho não existe")
        void shouldThrowWhenCartNotFound() {
            UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(1).build();
            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateCartItem("jose@test.com", 100L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção quando item não existe")
        void shouldThrowWhenCartItemNotFound() {
            UpdateCartItemRequest request = UpdateCartItemRequest.builder().quantity(1).build();
            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateCartItem("jose@test.com", 999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // removeCartItem()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("removeCartItem()")
    class RemoveCartItemTests {

        @Test
        @DisplayName("Deve remover item do carrinho com sucesso")
        void shouldRemoveCartItemSuccessfully() {
            cart.getItems().add(cartItem);
            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(cartItemRepository.findById(100L)).thenReturn(Optional.of(cartItem));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            cartService.removeCartItem("jose@test.com", 100L);

            verify(cartItemRepository).delete(cartItem);
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("Deve lançar exceção quando item não pertence ao carrinho do usuário")
        void shouldThrowWhenItemNotInUserCart() {
            Cart otherCart = Cart.builder().id(99L).user(user).items(new ArrayList<>()).build();
            cartItem.setCart(otherCart);

            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(cartItemRepository.findById(100L)).thenReturn(Optional.of(cartItem));

            assertThatThrownBy(() -> cartService.removeCartItem("jose@test.com", 100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("does not belong");

            verify(cartItemRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando carrinho não existe")
        void shouldThrowWhenCartNotFound() {
            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.removeCartItem("jose@test.com", 100L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // clearCart()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("clearCart()")
    class ClearCartTests {

        @Test
        @DisplayName("Deve limpar todos os itens do carrinho")
        void shouldClearAllCartItems() {
            cart.getItems().add(cartItem);
            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.of(cart));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            cartService.clearCart("jose@test.com");

            verify(cartItemRepository).deleteByCartId(1L);
            verify(cartRepository).save(cart);
            assertThat(cart.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Deve lançar exceção quando carrinho não existe")
        void shouldThrowWhenCartNotFound() {
            when(cartRepository.findByUserEmail("jose@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.clearCart("jose@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(cartItemRepository, never()).deleteByCartId(any());
        }
    }
}

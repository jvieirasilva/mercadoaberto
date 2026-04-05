package com.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.dto.AddToCartRequest;
import com.security.dto.CartItemResponse;
import com.security.dto.CartResponse;
import com.security.dto.UpdateCartItemRequest;
import com.security.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartController Tests")
class CartControllerTest {

    @Mock private CartService cartService;
    @InjectMocks private CartController cartController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication authentication;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
        objectMapper = new ObjectMapper();
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("jose@test.com");

        CartItemResponse item = new CartItemResponse();
        item.setCartItemId(1L);
        item.setProductId(10L);
        item.setQuantity(2);
        item.setPriceAtAddition(BigDecimal.valueOf(99.90));
        item.setSubtotal(BigDecimal.valueOf(199.80));

        cartResponse = new CartResponse();
        cartResponse.setItems(List.of(item));
        cartResponse.setTotalPrice(BigDecimal.valueOf(199.80));
        cartResponse.setTotalItems(1);
    }

    @Nested
    @DisplayName("POST /api/cart/add")
    class AddToCartTests {

        @Test
        @DisplayName("deve adicionar produto ao carrinho com sucesso")
        void shouldAddToCart() throws Exception {
            AddToCartRequest request = new AddToCartRequest();
            request.setProductId(10L);
            request.setQuantity(2);

            when(cartService.addToCart(eq("jose@test.com"), any(AddToCartRequest.class)))
                    .thenReturn(cartResponse);

            mockMvc.perform(post("/api/cart/add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(authentication))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalPrice").value(199.80));
        }
    }

    @Nested
    @DisplayName("GET /api/cart")
    class GetCartTests {

        @Test
        @DisplayName("deve retornar carrinho do utilizador")
        void shouldGetCart() throws Exception {
            when(cartService.getCart("jose@test.com")).thenReturn(cartResponse);

            mockMvc.perform(get("/api/cart").principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }
    }

    @Nested
    @DisplayName("PUT /api/cart/items/{id}")
    class UpdateCartItemTests {

        @Test
        @DisplayName("deve atualizar quantidade do item")
        void shouldUpdateCartItem() throws Exception {
            UpdateCartItemRequest request = new UpdateCartItemRequest();
            request.setQuantity(5);

            when(cartService.updateCartItem(eq("jose@test.com"), eq(1L), any(UpdateCartItemRequest.class)))
                    .thenReturn(cartResponse);

            mockMvc.perform(put("/api/cart/items/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .principal(authentication))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/cart/items/{id}")
    class RemoveCartItemTests {

        @Test
        @DisplayName("deve remover item do carrinho")
        void shouldRemoveCartItem() throws Exception {
            when(cartService.removeCartItem("jose@test.com", 1L)).thenReturn(cartResponse);

            mockMvc.perform(delete("/api/cart/items/1").principal(authentication))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/cart/clear")
    class ClearCartTests {

        @Test
        @DisplayName("deve limpar o carrinho")
        void shouldClearCart() throws Exception {
            doNothing().when(cartService).clearCart("jose@test.com");

            mockMvc.perform(delete("/api/cart/clear").principal(authentication))
                    .andExpect(status().isNoContent());
        }
    }
}

package com.security.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Cart Model Tests")
class CartTest {

    private Cart cart;
    private User user;
    private Product product1;
    private Product product2;
    private CartItem item1;
    private CartItem item2;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("jose@test.com")
                .role(Role.USER)
                .isActive(true)
                .isNotLocked(true)
                .build();

        product1 = new Product();
        product1.setId(10L);
        product1.setName("Produto A");
        product1.setPrice(new BigDecimal("20.00"));
        product1.setStockQuantity(new BigDecimal("100"));
        product1.setImages(new ArrayList<>());

        product2 = new Product();
        product2.setId(20L);
        product2.setName("Produto B");
        product2.setPrice(new BigDecimal("15.50"));
        product2.setStockQuantity(new BigDecimal("50"));
        product2.setImages(new ArrayList<>());

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>())
                .build();

        item1 = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product1)
                .quantity(3)
                .priceAtAddition(new BigDecimal("20.00"))
                .build();

        item2 = CartItem.builder()
                .id(2L)
                .cart(cart)
                .product(product2)
                .quantity(2)
                .priceAtAddition(new BigDecimal("15.50"))
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // getTotalPrice()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getTotalPrice()")
    class GetTotalPriceTests {

        @Test
        @DisplayName("Deve retornar ZERO quando carrinho está vazio")
        void shouldReturnZeroForEmptyCart() {
            assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Deve calcular total com um item")
        void shouldCalculateTotalWithOneItem() {
            cart.addItem(item1);

            // 3 x 20.00 = 60.00
            assertThat(cart.getTotalPrice()).isEqualByComparingTo("60.00");
        }

        @Test
        @DisplayName("Deve calcular total com múltiplos itens")
        void shouldCalculateTotalWithMultipleItems() {
            cart.addItem(item1); // 3 x 20.00 = 60.00
            cart.addItem(item2); // 2 x 15.50 = 31.00

            assertThat(cart.getTotalPrice()).isEqualByComparingTo("91.00");
        }

        @Test
        @DisplayName("Deve tratar item com priceAtAddition nulo como zero")
        void shouldTreatNullPriceAsZero() {
            CartItem nullPriceItem = CartItem.builder()
                    .id(3L)
                    .cart(cart)
                    .product(product1)
                    .quantity(1)
                    .priceAtAddition(null)
                    .build();

            cart.addItem(nullPriceItem);

            assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Deve tratar item com quantity nulo como zero")
        void shouldTreatNullQuantityAsZero() {
            CartItem nullQtyItem = CartItem.builder()
                    .id(3L)
                    .cart(cart)
                    .product(product1)
                    .quantity(null)
                    .priceAtAddition(new BigDecimal("20.00"))
                    .build();

            cart.addItem(nullQtyItem);

            assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getTotalItems()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getTotalItems()")
    class GetTotalItemsTests {

        @Test
        @DisplayName("Deve retornar 0 para carrinho vazio")
        void shouldReturnZeroForEmptyCart() {
            assertThat(cart.getTotalItems()).isZero();
        }

        @Test
        @DisplayName("Deve somar as quantidades de todos os itens")
        void shouldSumAllItemQuantities() {
            cart.addItem(item1); // qty=3
            cart.addItem(item2); // qty=2

            assertThat(cart.getTotalItems()).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve contar corretamente com item de quantidade 1")
        void shouldCountSingleQuantityItem() {
            CartItem singleItem = CartItem.builder()
                    .id(3L)
                    .cart(cart)
                    .product(product1)
                    .quantity(1)
                    .priceAtAddition(new BigDecimal("20.00"))
                    .build();

            cart.addItem(singleItem);

            assertThat(cart.getTotalItems()).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // addItem() / removeItem()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addItem() / removeItem()")
    class ItemManagementTests {

        @Test
        @DisplayName("Deve adicionar item ao carrinho e vincular cart ao item")
        void shouldAddItemAndSetCartReference() {
            CartItem newItem = CartItem.builder()
                    .id(10L)
                    .product(product1)
                    .quantity(1)
                    .priceAtAddition(new BigDecimal("20.00"))
                    .build();

            cart.addItem(newItem);

            assertThat(cart.getItems()).contains(newItem);
            assertThat(newItem.getCart()).isEqualTo(cart);
        }

        @Test
        @DisplayName("Deve remover item do carrinho e desvinculá-lo")
        void shouldRemoveItemAndClearCartReference() {
            cart.addItem(item1);
            assertThat(cart.getItems()).hasSize(1);

            cart.removeItem(item1);

            assertThat(cart.getItems()).isEmpty();
            assertThat(item1.getCart()).isNull();
        }

        @Test
        @DisplayName("Deve adicionar múltiplos itens ao carrinho")
        void shouldAddMultipleItems() {
            cart.addItem(item1);
            cart.addItem(item2);

            assertThat(cart.getItems()).hasSize(2);
            assertThat(cart.getItems()).containsExactlyInAnyOrder(item1, item2);
        }

        @Test
        @DisplayName("Deve remover apenas o item especificado")
        void shouldRemoveOnlySpecifiedItem() {
            cart.addItem(item1);
            cart.addItem(item2);

            cart.removeItem(item1);

            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems()).contains(item2);
            assertThat(cart.getItems()).doesNotContain(item1);
        }
    }
}

@DisplayName("CartItem Model Tests")
class CartItemTest {

    @Nested
    @DisplayName("getSubtotal()")
    class GetSubtotalTests {

        @Test
        @DisplayName("Deve calcular subtotal corretamente")
        void shouldCalculateSubtotalCorrectly() {
            CartItem item = CartItem.builder()
                    .id(1L)
                    .quantity(3)
                    .priceAtAddition(new BigDecimal("25.50"))
                    .build();

            // 3 x 25.50 = 76.50
            assertThat(item.getSubtotal()).isEqualByComparingTo("76.50");
        }

        @Test
        @DisplayName("Deve retornar ZERO quando priceAtAddition é nulo")
        void shouldReturnZeroWhenPriceIsNull() {
            CartItem item = CartItem.builder()
                    .id(1L)
                    .quantity(3)
                    .priceAtAddition(null)
                    .build();

            assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Deve retornar ZERO quando quantity é nulo")
        void shouldReturnZeroWhenQuantityIsNull() {
            CartItem item = CartItem.builder()
                    .id(1L)
                    .quantity(null)
                    .priceAtAddition(new BigDecimal("25.50"))
                    .build();

            assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Deve calcular subtotal corretamente com quantidade = 1")
        void shouldCalculateSubtotalForQuantityOne() {
            CartItem item = CartItem.builder()
                    .id(1L)
                    .quantity(1)
                    .priceAtAddition(new BigDecimal("99.99"))
                    .build();

            assertThat(item.getSubtotal()).isEqualByComparingTo("99.99");
        }

        @Test
        @DisplayName("Deve calcular subtotal com preços decimais de alta precisão")
        void shouldHandleHighPrecisionPrices() {
            CartItem item = CartItem.builder()
                    .id(1L)
                    .quantity(4)
                    .priceAtAddition(new BigDecimal("10.995"))
                    .build();

            // 4 x 10.995 = 43.980
            assertThat(item.getSubtotal()).isEqualByComparingTo("43.98");
        }

        @Test
        @DisplayName("Deve calcular subtotal zero para preço zero")
        void shouldReturnZeroForZeroPrice() {
            CartItem item = CartItem.builder()
                    .id(1L)
                    .quantity(10)
                    .priceAtAddition(BigDecimal.ZERO)
                    .build();

            assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}

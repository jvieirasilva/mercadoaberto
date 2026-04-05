package com.security.controller;

import com.security.dto.ProductResponseDTO;
import com.security.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Mock private ProductService productService;
    @InjectMocks private ProductController productController;

    private MockMvc mockMvc;
    private ProductResponseDTO productResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        productResponse = new ProductResponseDTO();
        productResponse.setId(1L);
        productResponse.setName("Produto Teste");
        productResponse.setPrice(BigDecimal.valueOf(50.00));
    }

    private Page<ProductResponseDTO> buildPage(ProductResponseDTO item) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        return new PageImpl<>(List.of(item), pageable, 1);
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("deve retornar produto por ID")
        void shouldGetById() throws Exception {
            when(productService.getById(1L)).thenReturn(productResponse);

            mockMvc.perform(get("/api/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/products")
    class ListTests {

        @Test
        @DisplayName("deve listar produtos paginados")
        void shouldListProducts() throws Exception {
            when(productService.list(any(Pageable.class))).thenReturn(buildPage(productResponse));

            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/products/search")
    class SearchTests {

        @Test
        @DisplayName("deve pesquisar produtos por termo")
        void shouldSearchProducts() throws Exception {
            when(productService.search(anyString(), any(Pageable.class))).thenReturn(buildPage(productResponse));

            mockMvc.perform(get("/api/products/search").param("term", "teste"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/products/searchByCompany")
    class SearchByCompanyTests {

        @Test
        @DisplayName("deve pesquisar produtos por empresa")
        void shouldSearchByCompany() throws Exception {
            when(productService.searchByCompany(anyLong(), anyString(), any(Pageable.class)))
                    .thenReturn(buildPage(productResponse));

            mockMvc.perform(get("/api/products/searchByCompany")
                            .param("companyId", "1")
                            .param("term", "teste"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteTests {

        @Test
        @DisplayName("deve deletar produto")
        void shouldDeleteProduct() throws Exception {
            doNothing().when(productService).delete(1L);

            mockMvc.perform(delete("/api/products/1"))
                    .andExpect(status().isNoContent());
        }
    }
}

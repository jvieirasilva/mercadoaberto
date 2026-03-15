package com.security.service;

import com.security.dto.ProductRequestDTO;
import com.security.dto.ProductResponseDTO;
import com.security.exception.ResourceNotFoundException;
import com.security.model.Company;
import com.security.model.Product;
import com.security.repository.CompanyRepository;
import com.security.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private S3UploadService s3UploadService;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductRequestDTO requestDTO;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("Empresa Teste");
        company.setProducts(new ArrayList<>());

        product = new Product();
        product.setId(100L);
        product.setName("Produto Teste");
        product.setPrice(new BigDecimal("49.99"));
        product.setStockQuantity(new BigDecimal("50.00"));
        product.setDescription("Descrição do produto");
        product.setIsActive(true);
        product.setImages(new ArrayList<>());
        product.setCompany(company);

        requestDTO = ProductRequestDTO.builder()
                .name("Produto Teste")
                .price(new BigDecimal("49.99"))
                .stockQuantity(new BigDecimal("50.00"))
                .description("Descrição do produto")
                .isActive(true)
                .images(new ArrayList<>())
                .productImages(new ArrayList<>())
                .companyId(1L)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // create()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Deve criar produto com sucesso sem imagens")
        void shouldCreateProductWithoutImages() {
            when(companyRepository.findByIdWithProducts(1L)).thenReturn(Optional.of(company));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            ProductResponseDTO response = productService.create(requestDTO);

            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Produto Teste");
            assertThat(response.getPrice()).isEqualByComparingTo("49.99");
            assertThat(response.getStockQuantity()).isEqualByComparingTo("50.00");
            assertThat(response.getIsActive()).isTrue();

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando empresa não existe")
        void shouldThrowWhenCompanyNotFound() {
            when(companyRepository.findByIdWithProducts(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.create(requestDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("1");

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar produto com isActive=true quando não informado")
        void shouldDefaultToActiveTrueWhenNotProvided() {
            requestDTO.setIsActive(null);
            when(companyRepository.findByIdWithProducts(1L)).thenReturn(Optional.of(company));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            when(productRepository.save(captor.capture())).thenReturn(product);

            productService.create(requestDTO);

            assertThat(captor.getValue().getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Deve associar a empresa correta ao produto")
        void shouldAssignCorrectCompany() {
            when(companyRepository.findByIdWithProducts(1L)).thenReturn(Optional.of(company));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            when(productRepository.save(captor.capture())).thenReturn(product);

            productService.create(requestDTO);

            assertThat(captor.getValue().getCompany().getId()).isEqualTo(1L);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getById()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Deve retornar produto quando encontrado")
        void shouldReturnProductWhenFound() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));

            ProductResponseDTO response = productService.getById(100L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getName()).isEqualTo("Produto Teste");
            assertThat(response.getDescription()).isEqualTo("Descrição do produto");
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // list()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("list()")
    class ListTests {

        @Test
        @DisplayName("Deve retornar página de produtos")
        void shouldReturnPagedProducts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);
            when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

            Page<ProductResponseDTO> result = productService.list(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Produto Teste");
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não há produtos")
        void shouldReturnEmptyPageWhenNoProducts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(productRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            Page<ProductResponseDTO> result = productService.list(pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // search()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("search()")
    class SearchTests {

        @Test
        @DisplayName("Deve buscar produtos por termo com paginação")
        void shouldSearchProductsByTerm() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
            Page<Product> resultPage = new PageImpl<>(List.of(product), pageable, 1);
            when(productRepository.searchByNameOrDescription(eq("teste"), any(Pageable.class)))
                    .thenReturn(resultPage);

            Page<ProductResponseDTO> result = productService.search("teste", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Deve tratar termo nulo como string vazia")
        void shouldTreatNullTermAsEmptyString() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
            Page<Product> page = new PageImpl<>(List.of(), pageable, 0);
            when(productRepository.searchByNameOrDescription(eq(""), any(Pageable.class)))
                    .thenReturn(page);

            Page<ProductResponseDTO> result = productService.search(null, pageable);

            assertThat(result).isNotNull();
            verify(productRepository).searchByNameOrDescription(eq(""), any());
        }

        @Test
        @DisplayName("Deve trimar espaços do termo de busca")
        void shouldTrimSearchTerm() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
            Page<Product> page = new PageImpl<>(List.of(), pageable, 0);
            when(productRepository.searchByNameOrDescription(eq("teste"), any(Pageable.class)))
                    .thenReturn(page);

            productService.search("  teste  ", pageable);

            verify(productRepository).searchByNameOrDescription(eq("teste"), any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // searchByCompany()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("searchByCompany()")
    class SearchByCompanyTests {

        @Test
        @DisplayName("Deve buscar produtos de uma empresa por termo")
        void shouldSearchProductsByCompany() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
            Page<Product> resultPage = new PageImpl<>(List.of(product), pageable, 1);
            when(productRepository.searchByCompanyId(eq(1L), eq(""), any(Pageable.class)))
                    .thenReturn(resultPage);

            Page<ProductResponseDTO> result = productService.searchByCompany(1L, "", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando empresa não tem produtos")
        void shouldReturnEmptyWhenCompanyHasNoProducts() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("id")));
            Page<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(productRepository.searchByCompanyId(eq(99L), any(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<ProductResponseDTO> result = productService.searchByCompany(99L, "anything", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // update()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Deve atualizar produto com sucesso")
        void shouldUpdateProductSuccessfully() {
            ProductRequestDTO updateDTO = ProductRequestDTO.builder()
                    .name("Produto Atualizado")
                    .price(new BigDecimal("79.99"))
                    .stockQuantity(new BigDecimal("25.00"))
                    .description("Nova descrição")
                    .isActive(true)
                    .images(new ArrayList<>())
                    .productImages(new ArrayList<>())
                    .companyId(1L)
                    .build();

            when(productRepository.findById(100L)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            ProductResponseDTO response = productService.update(100L, updateDTO);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Produto Atualizado");
            assertThat(captor.getValue().getPrice()).isEqualByComparingTo("79.99");
        }

        @Test
        @DisplayName("Deve lançar exceção quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(999L, requestDTO))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // delete()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar produto com sucesso")
        void shouldDeleteProductSuccessfully() {
            when(productRepository.existsById(100L)).thenReturn(true);

            productService.delete(100L);

            verify(productRepository).deleteById(100L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> productService.delete(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");

            verify(productRepository, never()).deleteById(any());
        }
    }
}

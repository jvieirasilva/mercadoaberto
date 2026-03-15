package com.security.service;

import com.security.dto.CompanyRequestDTO;
import com.security.dto.CompanyResponseDTO;
import com.security.model.Company;
import com.security.model.Product;
import com.security.model.Role;
import com.security.model.User;
import com.security.repository.CompanyRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService Tests")
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    private Company company;
    private CompanyRequestDTO requestDTO;
    private User adminUser;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("Empresa Teste");
        company.setTradeName("Teste Ltda");
        company.setNif("123456789");
        company.setEmail("empresa@test.com");
        company.setPhone("+351912345678");
        company.setAddress("Rua Teste, 123");
        company.setPostalCode("4000-000");
        company.setCity("Porto");
        company.setCountry("Portugal");
        company.setIsActive(true);
        company.setProducts(new ArrayList<>());
        company.setAdministrators(new ArrayList<>());

        requestDTO = CompanyRequestDTO.builder()
                .companyName("Empresa Teste")
                .tradeName("Teste Ltda")
                .nif("123456789")
                .email("empresa@test.com")
                .phone("+351912345678")
                .address("Rua Teste, 123")
                .postalCode("4000-000")
                .city("Porto")
                .country("Portugal")
                .isActive(true)
                .build();

        adminUser = User.builder()
                .id(10L)
                .email("admin@test.com")
                .role(Role.ADMIN)
                .company(company)
                .isActive(true)
                .isNotLocked(true)
                .build();

        superAdminUser = User.builder()
                .id(20L)
                .email("superadmin@test.com")
                .role(Role.USER) // usa USER como SUPER_ADMIN não existe no enum — testa com USER
                .isActive(true)
                .isNotLocked(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // createCompany()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createCompany()")
    class CreateCompanyTests {

        @Test
        @DisplayName("Deve criar empresa com sucesso")
        void shouldCreateCompanySuccessfully() {
            when(companyRepository.existsByNif("123456789")).thenReturn(false);
            when(companyRepository.findByEmail("empresa@test.com")).thenReturn(Optional.empty());
            when(companyRepository.save(any(Company.class))).thenReturn(company);

            CompanyResponseDTO response = companyService.createCompany(requestDTO);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getCompanyName()).isEqualTo("Empresa Teste");
            assertThat(response.getNif()).isEqualTo("123456789");

            verify(companyRepository).save(any(Company.class));
        }

        @Test
        @DisplayName("Deve usar Portugal como país padrão quando país não é informado")
        void shouldDefaultToPortugalWhenCountryIsNull() {
            requestDTO.setCountry(null);
            when(companyRepository.existsByNif(any())).thenReturn(false);
            when(companyRepository.findByEmail(any())).thenReturn(Optional.empty());

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            when(companyRepository.save(captor.capture())).thenReturn(company);

            companyService.createCompany(requestDTO);

            assertThat(captor.getValue().getCountry()).isEqualTo("Portugal");
        }

        @Test
        @DisplayName("Deve lançar exceção quando NIF já existe")
        void shouldThrowWhenNifAlreadyExists() {
            when(companyRepository.existsByNif("123456789")).thenReturn(true);

            assertThatThrownBy(() -> companyService.createCompany(requestDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("123456789");

            verify(companyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando email já existe em outra empresa")
        void shouldThrowWhenEmailAlreadyExists() {
            Company otherCompany = new Company();
            otherCompany.setId(99L);

            when(companyRepository.existsByNif("123456789")).thenReturn(false);
            when(companyRepository.findByEmail("empresa@test.com"))
                    .thenReturn(Optional.of(otherCompany));

            assertThatThrownBy(() -> companyService.createCompany(requestDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("empresa@test.com");
        }

        @Test
        @DisplayName("Deve criar empresa com isActive=true por padrão quando não informado")
        void shouldDefaultToActiveTrueWhenNull() {
            requestDTO.setIsActive(null);
            when(companyRepository.existsByNif(any())).thenReturn(false);
            when(companyRepository.findByEmail(any())).thenReturn(Optional.empty());

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            when(companyRepository.save(captor.capture())).thenReturn(company);

            companyService.createCompany(requestDTO);

            assertThat(captor.getValue().getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Deve não validar email quando ele é nulo")
        void shouldSkipEmailValidationWhenNull() {
            requestDTO.setEmail(null);
            when(companyRepository.existsByNif("123456789")).thenReturn(false);
            when(companyRepository.save(any())).thenReturn(company);

            CompanyResponseDTO response = companyService.createCompany(requestDTO);

            assertThat(response).isNotNull();
            verify(companyRepository, never()).findByEmail(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateCompany()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateCompany()")
    class UpdateCompanyTests {

        @Test
        @DisplayName("ADMIN deve poder atualizar sua própria empresa")
        void adminShouldUpdateOwnCompany() {
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
            when(companyRepository.save(any())).thenReturn(company);

            CompanyResponseDTO response = companyService.updateCompany(1L, requestDTO, adminUser);

            assertThat(response).isNotNull();
            verify(companyRepository).save(any(Company.class));
        }

        @Test
        @DisplayName("ADMIN não deve poder atualizar empresa de outro ADMIN")
        void adminShouldNotUpdateAnotherCompany() {
            Company otherCompany = new Company();
            otherCompany.setId(99L);
            adminUser.setCompany(otherCompany); // admin pertence a outra empresa

            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

            assertThatThrownBy(() -> companyService.updateCompany(1L, requestDTO, adminUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("permission");
        }

        @Test
        @DisplayName("Deve lançar exceção quando empresa não existe")
        void shouldThrowWhenCompanyNotFound() {
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> companyService.updateCompany(999L, requestDTO, adminUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Deve lançar exceção quando novo NIF já pertence a outra empresa")
        void shouldThrowWhenNewNifBelongsToAnotherCompany() {
            requestDTO.setNif("999999999"); // NIF diferente
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
            when(companyRepository.existsByNifAndIdNot("999999999", 1L)).thenReturn(true);

            assertThatThrownBy(() -> companyService.updateCompany(1L, requestDTO, adminUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("999999999");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getCompanyById()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getCompanyById()")
    class GetCompanyByIdTests {

        @Test
        @DisplayName("Deve retornar empresa quando encontrada")
        void shouldReturnCompanyWhenFound() {
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

            CompanyResponseDTO response = companyService.getCompanyById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getNif()).isEqualTo("123456789");
            assertThat(response.getCity()).isEqualTo("Porto");
        }

        @Test
        @DisplayName("Deve lançar exceção quando empresa não existe")
        void shouldThrowWhenNotFound() {
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> companyService.getCompanyById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("999");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getCompanyByIdWithStats()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getCompanyByIdWithStats()")
    class GetCompanyWithStatsTests {

        @Test
        @DisplayName("Deve retornar estatísticas quando usuário é ADMIN da empresa")
        void shouldReturnStatsForAdminOfOwnCompany() {
            Product activeProduct = new Product();
            activeProduct.setIsActive(true);
            company.getProducts().add(activeProduct);

            User adminInCompany = User.builder()
                    .id(10L)
                    .role(Role.ADMIN)
                    .company(company)
                    .build();
            company.getAdministrators().add(adminInCompany);

            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

            CompanyResponseDTO response = companyService.getCompanyByIdWithStats(1L, adminUser);

            assertThat(response.getTotalProducts()).isEqualTo(1L);
            assertThat(response.getActiveProducts()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ADMIN tenta ver stats de outra empresa")
        void shouldThrowWhenAdminTriesOtherCompanyStats() {
            Company otherCompany = new Company();
            otherCompany.setId(99L);
            User otherAdmin = User.builder()
                    .id(30L)
                    .role(Role.ADMIN)
                    .company(otherCompany)
                    .build();

            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

            assertThatThrownBy(() -> companyService.getCompanyByIdWithStats(1L, otherAdmin))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("permission");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getAllCompanies()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAllCompanies()")
    class GetAllCompaniesTests {

        @Test
        @DisplayName("USER deve ver todas as empresas")
        void userShouldSeeAllCompanies() {
            when(companyRepository.findAll()).thenReturn(List.of(company));

            List<CompanyResponseDTO> result = companyService.getAllCompanies(superAdminUser);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("ADMIN deve ver apenas sua própria empresa")
        void adminShouldSeeOnlyOwnCompany() {
            List<CompanyResponseDTO> result = companyService.getAllCompanies(adminUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(companyRepository, never()).findAll();
        }

        @Test
        @DisplayName("ADMIN sem empresa deve ver lista vazia")
        void adminWithoutCompanyShouldSeeEmptyList() {
            adminUser.setCompany(null);

            List<CompanyResponseDTO> result = companyService.getAllCompanies(adminUser);

            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // activateCompany() / deactivateCompany()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("activateCompany() / deactivateCompany()")
    class ActivationTests {

        @Test
        @DisplayName("Deve ativar empresa com sucesso")
        void shouldActivateCompany() {
            company.setIsActive(false);
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
            when(companyRepository.save(any())).thenReturn(company);

            CompanyResponseDTO response = companyService.activateCompany(1L);

            verify(companyRepository).save(argThat(c -> c.getIsActive()));
        }

        @Test
        @DisplayName("Deve desativar empresa com sucesso")
        void shouldDeactivateCompany() {
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
            when(companyRepository.save(any())).thenReturn(company);

            companyService.deactivateCompany(1L);

            verify(companyRepository).save(argThat(c -> !c.getIsActive()));
        }

        @Test
        @DisplayName("Deve lançar exceção ao ativar empresa inexistente")
        void shouldThrowWhenActivatingNonExistentCompany() {
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> companyService.activateCompany(999L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // deleteCompany()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteCompany()")
    class DeleteCompanyTests {

        @Test
        @DisplayName("Deve deletar empresa sem produtos e sem admins")
        void shouldDeleteCompanyWithNoRelationships() {
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

            companyService.deleteCompany(1L);

            verify(companyRepository).delete(company);
        }

        @Test
        @DisplayName("Deve lançar exceção quando empresa tem produtos associados")
        void shouldThrowWhenCompanyHasProducts() {
            Product product = new Product();
            company.getProducts().add(product);
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

            assertThatThrownBy(() -> companyService.deleteCompany(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("products");

            verify(companyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando empresa tem administradores")
        void shouldThrowWhenCompanyHasAdministrators() {
            company.getAdministrators().add(adminUser);
            when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

            assertThatThrownBy(() -> companyService.deleteCompany(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("administrators");

            verify(companyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando empresa não existe")
        void shouldThrowWhenNotFound() {
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> companyService.deleteCompany(999L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // hardDeleteCompany()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("hardDeleteCompany()")
    class HardDeleteTests {

        @Test
        @DisplayName("Deve fazer hard delete com sucesso")
        void shouldHardDeleteSuccessfully() {
            when(companyRepository.existsById(1L)).thenReturn(true);

            companyService.hardDeleteCompany(1L);

            verify(companyRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando empresa não existe")
        void shouldThrowWhenNotFound() {
            when(companyRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> companyService.hardDeleteCompany(999L))
                    .isInstanceOf(RuntimeException.class);

            verify(companyRepository, never()).deleteById(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // countAllCompanies() / countActiveCompanies()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("count methods")
    class CountTests {

        @Test
        @DisplayName("Deve retornar total de empresas")
        void shouldReturnTotalCount() {
            when(companyRepository.count()).thenReturn(5L);
            assertThat(companyService.countAllCompanies()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Deve retornar total de empresas ativas")
        void shouldReturnActiveCount() {
            when(companyRepository.countByIsActiveTrue()).thenReturn(3L);
            assertThat(companyService.countActiveCompanies()).isEqualTo(3L);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // searchCompaniesByName() / getCompaniesByCity()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("searchCompaniesByName() / getCompaniesByCity()")
    class SearchTests {

        @Test
        @DisplayName("Deve buscar empresas por nome")
        void shouldSearchByName() {
            when(companyRepository.findByCompanyNameContainingIgnoreCase("Teste"))
                    .thenReturn(List.of(company));

            List<CompanyResponseDTO> result = companyService.searchCompaniesByName("Teste");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCompanyName()).isEqualTo("Empresa Teste");
        }

        @Test
        @DisplayName("Deve buscar empresas por cidade")
        void shouldGetByCity() {
            when(companyRepository.findByCity("Porto")).thenReturn(List.of(company));

            List<CompanyResponseDTO> result = companyService.getCompaniesByCity("Porto");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCity()).isEqualTo("Porto");
        }
    }
}

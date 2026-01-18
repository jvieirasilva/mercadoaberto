package com.security.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.security.dto.ProductRequestDTO;
import com.security.dto.ProductResponseDTO;
import com.security.exception.ResourceNotFoundException;
import com.security.model.Company;
import com.security.model.Product;
import com.security.repository.CompanyRepository;
import com.security.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final S3UploadService s3UploadService;
    private final CompanyRepository companyRepository;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    @Transactional                   
    public ProductResponseDTO create(ProductRequestDTO dto) {
        Product product = new Product();
        applyDtoToEntity(dto, product);

        Company company = companyRepository.findByIdWithProducts(dto.getCompanyId())
        	    .orElseThrow(() -> new RuntimeException("Company não encontrada: " + dto.getCompanyId()));
        product.setCompany(company);
        Product saved = productRepository.save(product);
        
        try {
            List<MultipartFile> images = dto.getProductImages();
            
           // company

            if (images != null && !images.isEmpty()) {

                List<String> uploadedUrls = new ArrayList<>();

                for (int i = 0; i < images.size(); i++) {
                    MultipartFile image = images.get(i);

                    if (image == null || image.isEmpty()) {
                        continue;
                    }

                    String originalFilename = image.getOriginalFilename();
                    LOGGER.info("Imagem [{}] - Nome original: {}", i, originalFilename);

                    // Extrair extensão
                    String fileExtension;
                    if (originalFilename != null && originalFilename.contains(".")) {
                        int lastDotIndex = originalFilename.lastIndexOf(".");
                        fileExtension = originalFilename.substring(lastDotIndex).toLowerCase();
                    } else {
                        fileExtension = ".png";
                    }

                    // Validar extensões permitidas
                    if (!fileExtension.matches("\\.(png|jpg|jpeg|webp)$")) {
                        LOGGER.warn(
                            "Extensão não suportada ({}) para arquivo {}. Usando .png",
                            fileExtension,
                            originalFilename
                        );
                        fileExtension = ".png";
                    }

                    // Nome final: <productId>_<ordem>.<ext>
                    String index = String.format("%02d", i + 1);
                    String newFilename = product.getId() + "_" + index + fileExtension;

                    LOGGER.info(
                        "Imagem [{}] - Nome final a ser enviado ao S3: {}",
                        i,
                        newFilename
                    );

                    String imageUrl = s3UploadService.uploadProductmage(
                        newFilename,
                        image
                    );

                    LOGGER.info(
                        "Imagem [{}] - URL retornada do S3: {}",
                        i,
                        imageUrl
                    );

                    uploadedUrls.add(imageUrl);
                }

                // 🔁 UPDATE padrão → substitui imagens antigas
                product.setImages(uploadedUrls);

                // ➕ Se quiser apenas adicionar novas:
                // product.getImages().addAll(uploadedUrls);

                LOGGER.info(
                    "Gravou {} imagem(ns) para product: {}",
                    uploadedUrls.size(),
                    product.getId()
                );
            }

        } catch (Exception e) {
            LOGGER.error(
                "Erro ao gravar arquivos para product: " + product.getId(),
                e
            );
        }


        
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> list(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> search(String term, Pageable pageable) {
        String normalized = (term == null) ? "" : term.trim();

        Pageable safe = sanitizePageable(pageable);
        
        Page<ProductResponseDTO> ret = productRepository.searchByNameOrDescription(normalized, safe)
                .map(this::toResponse);

        return ret;
    }
    
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchByCompany(Long idCompany, String term, Pageable pageable) {
        String normalized = (term == null) ? "" : term.trim();

        Pageable safe = sanitizePageable(pageable);
        
        Page<ProductResponseDTO> ret = productRepository.searchByCompanyId(idCompany,normalized, safe)
                .map(this::toResponse);

        return ret;
    }
    
    

    @Transactional
    public ProductResponseDTO update(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        applyDtoToEntity(dto, product);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private void applyDtoToEntity(ProductRequestDTO dto, Product product) {
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setDescription(dto.getDescription());
        product.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE);

        // garante lista não-null
        product.setImages(dto.getImages() != null ? new ArrayList<>(dto.getImages()) : new ArrayList<>());
    }

    private ProductResponseDTO toResponse(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .images(product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .description(product.getDescription())
                .isActive(product.getIsActive())
                .build();
    }
    
    private Pageable sanitizePageable(Pageable pageable) {
        var allowed = List.of("id", "name", "price", "stockQuantity", "description", "isActive");

        Sort safeSort = Sort.unsorted();
        for (Sort.Order order : pageable.getSort()) {
            if (allowed.contains(order.getProperty())) {
                safeSort = safeSort.and(Sort.by(order));
            }
        }

        if (safeSort.isUnsorted()) {
            safeSort = Sort.by(Sort.Order.desc("id"));
        }

        int size = pageable.getPageSize() <= 0 ? 10 : pageable.getPageSize();
        int page = Math.max(pageable.getPageNumber(), 0);

        return PageRequest.of(page, size, safeSort);
    }
}

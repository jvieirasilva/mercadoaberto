package com.security.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    private String name;

    private List<String> images; // URLs das imagens salvas

    @NotBlank(message = "O preço é obrigatório")
    private String price;

    private List<MultipartFile> imageFiles; // Arquivos recebidos no upload (não será salvo no banco)
}
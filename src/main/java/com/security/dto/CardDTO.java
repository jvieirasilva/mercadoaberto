package com.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDTO {

	private Long id;
	private String numero;
	private String dataValidade;
	private String codigoSeguranca;
	private String nome;

}
package com.security.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.security.dto.CardDTO;
import com.security.service.CardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/card")
@RequiredArgsConstructor
@Tag(name = "Card", description = "Gestão de cartão de credito")
public class CardController {
	private final CardService cardService;

	 @Operation(summary = "Adicionar um novo cartão ")
	    @ApiResponses({
	        @ApiResponse(responseCode = "201", description = "Cartão adicionado com sucesso"),
	        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
	    })
	    @PostMapping
	    public ResponseEntity<?> criar(@Valid @RequestBody CardDTO cardDTO) {
		    CardDTO cardDTOReturn = cardService.criar(cardDTO);
	        return ResponseEntity.status(HttpStatus.CREATED).body(cardDTOReturn);
	    }
	 @Operation(summary = "Eliminar cartao por ID")
	    @ApiResponses({
	        @ApiResponse(responseCode = "204", description = "Cartao eliminado com sucesso"),
	        @ApiResponse(responseCode = "404", description = "Cartao não encontrado", content = @Content)
	    })
	    @DeleteMapping("/{id}")
	    public ResponseEntity<?> eliminar(
	            @Parameter(description = "ID do customer") @PathVariable String id) {
		 return ResponseEntity.status(HttpStatus.CREATED).body("Passei em deletar cartao endpoint");
	    }
	
}

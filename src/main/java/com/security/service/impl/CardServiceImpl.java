package com.security.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.security.dto.CardDTO;
import com.security.model.Card;
import com.security.repository.CardRepository;
import com.security.service.CardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CardServiceImpl implements CardService {

	private final CardRepository cardRepository;
	
	

	@Override
	public CardDTO criar(CardDTO cardDTO) {
		
		Card card = new Card();
		card.setCodigoSeguranca(cardDTO.getCodigoSeguranca());
		card.setNome(cardDTO.getNome());
		card.setDataValidade(cardDTO.getDataValidade());
		card.setNumero(cardDTO.getNumero());
		card.setId(cardDTO.getId());

		Card cardReturn =  cardRepository.save(card);
		
		CardDTO carDTOReturn = new CardDTO();
		carDTOReturn.setCodigoSeguranca(cardReturn.getCodigoSeguranca());
		carDTOReturn.setDataValidade(cardReturn.getDataValidade());
		carDTOReturn.setId(cardReturn.getId());
		carDTOReturn.setNome(cardReturn.getNome());
		carDTOReturn.setId(cardReturn.getId());
		carDTOReturn.setNumero(cardReturn.getNumero());
		return carDTOReturn;

	}

}

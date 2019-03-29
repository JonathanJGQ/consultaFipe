package com.sif.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.model.Folha;
import com.sif.repository.FolhaRepository;
import com.sif.repository.specification.FolhaSpecification;

@Service
public class FolhaService {

	@Autowired
	FolhaRepository folhaRepository;
	
	public ResponseEntity<List<Folha>> getAll() {
		return Optional
				.ofNullable(folhaRepository.findAll(new FolhaSpecification().findByStatus()))
				.map(folha -> ResponseEntity.ok().body(folha))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Folha> findById(Long id) {
		return Optional
				.ofNullable(folhaRepository.findById(id).get())
				.map(folha -> ResponseEntity.ok().body(folha))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Folha> create(Folha folha) {
		
		validarFolha(folha);
		
		return Optional
				.ofNullable(folhaRepository.save(folha))
				.map(folhaAux -> ResponseEntity.ok().body(folhaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Folha> update(Folha folha,Long id) {
		
		validarFolha(folha);
		
		Folha folhaSave = folhaRepository.findById(id).get();
		if(folhaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long folhaId = folhaSave.getId();
		
		BeanUtils.copyProperties(folha, folhaSave);
		folhaSave.setId(folhaId);
		
		return Optional
				.ofNullable(folhaRepository.save(folhaSave))
				.map(folhaAux -> ResponseEntity.ok().body(folhaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Folha> delete(Long id) {
		
		Folha folhaSave = folhaRepository.findById(id).get();
		if(folhaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		folhaSave.setStatus(false);
		
		return Optional
				.ofNullable(folhaRepository.save(folhaSave))
				.map(folhaAux -> ResponseEntity.ok().body(folhaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarFolha(Folha folha) throws GenericException {
		
		if(folha.getOrgao() == null) {
			throw new GenericException("Erro","Orgão não pode ser nulo");
		}
		if(folha.getPeriodo() == null) {
			throw new GenericException("Erro","Período não pode ser nulo");
		}
	}	
	
}

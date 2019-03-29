package com.sif.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.model.ItensFolha;
import com.sif.repository.ItensFolhaRepository;
import com.sif.repository.specification.ItensFolhaSpecification;

@Service
public class ItensFolhaService {

	@Autowired
	ItensFolhaRepository itensFolhaRepository;
	
	public ResponseEntity<List<ItensFolha>> getAll() {
		return Optional
				.ofNullable(itensFolhaRepository.findAll(new ItensFolhaSpecification().findByStatus()))
				.map(itensFolha -> ResponseEntity.ok().body(itensFolha))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<ItensFolha> findById(Long id) {
		return Optional
				.ofNullable(itensFolhaRepository.findById(id).get())
				.map(itensFolha -> ResponseEntity.ok().body(itensFolha))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<ItensFolha> create(ItensFolha itensFolha) {
		
		validarItensFolha(itensFolha);
		
		return Optional
				.ofNullable(itensFolhaRepository.save(itensFolha))
				.map(itensFolhaAux -> ResponseEntity.ok().body(itensFolhaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<ItensFolha> update(ItensFolha itensFolha,Long id) {
		
		validarItensFolha(itensFolha);
		
		ItensFolha itensFolhaSave = itensFolhaRepository.findById(id).get();
		if(itensFolhaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long clienteMargemId = itensFolhaSave.getId();
		
		BeanUtils.copyProperties(itensFolha, itensFolhaSave);
		itensFolhaSave.setId(clienteMargemId);
		
		return Optional
				.ofNullable(itensFolhaRepository.save(itensFolhaSave))
				.map(itensFolhaAux -> ResponseEntity.ok().body(itensFolhaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<ItensFolha> delete(Long id) {
		
		ItensFolha itensFolhaSave = itensFolhaRepository.findById(id).get();
		if(itensFolhaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		itensFolhaSave.setStatus(false);
		
		return Optional
				.ofNullable(itensFolhaRepository.save(itensFolhaSave))
				.map(itensFolhaAux -> ResponseEntity.ok().body(itensFolhaAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarItensFolha(ItensFolha itensFolha) throws GenericException {
		
		if(itensFolha.getContrato() == null) {
			throw new GenericException("Erro","Contrato não pode ser nulo");
		}
		if(itensFolha.getFolha() == null) {
			throw new GenericException("Erro","Folha não pode ser nulo");
		}
		if(itensFolha.getUsuario() == null) {
			throw new GenericException("Erro","Usuário não pode ser nulo");
		}
		if(itensFolha.getSecretaria() == null) {
			throw new GenericException("Erro","Secretaria não pode ser nulo");
		}
	}	
	
}

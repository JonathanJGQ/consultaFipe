package com.sif.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.model.Endereco;
import com.sif.repository.EnderecoRepository;
import com.sif.repository.specification.EnderecoSpecification;

@Service
public class EnderecoService {

	@Autowired
	EnderecoRepository enderecoRepository;
	
	public ResponseEntity<List<Endereco>> getAll() {
		return Optional
				.ofNullable(enderecoRepository.findAll(new EnderecoSpecification().findByStatus()))
				.map(endereco -> ResponseEntity.ok().body(endereco))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Endereco> findById(Long id) {
		return Optional
				.ofNullable(enderecoRepository.findById(id).get())
				.map(endereco -> ResponseEntity.ok().body(endereco))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Endereco> create(Endereco endereco) {
		
		validarEndereco(endereco);
		
		return Optional
				.ofNullable(enderecoRepository.save(endereco))
				.map(enderecoAux -> ResponseEntity.ok().body(enderecoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Endereco> update(Endereco endereco,Long id) {
		
		validarEndereco(endereco);
		
		Endereco enderecoSave = enderecoRepository.findById(id).get();
		if(enderecoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long enderecoId = enderecoSave.getId();
		
		BeanUtils.copyProperties(endereco, enderecoSave);
		enderecoSave.setId(enderecoId);
		
		return Optional
				.ofNullable(enderecoRepository.save(enderecoSave))
				.map(enderecoAux -> ResponseEntity.ok().body(enderecoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Endereco> delete(Long id) {
		
		Endereco enderecoSave = enderecoRepository.findById(id).get();
		if(enderecoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		enderecoSave.setStatus(false);
		
		return Optional
				.ofNullable(enderecoRepository.save(enderecoSave))
				.map(enderecoAux -> ResponseEntity.ok().body(enderecoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarEndereco(Endereco endereco) throws GenericException {
		
		if(endereco.getCidade() == null) {
			throw new GenericException("Erro","Cidade não pode ser nulo");
		}
	}

	public Endereco save(Endereco endereco) {
		return enderecoRepository.save(endereco);
		
	}	
	
}

package com.sif.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.model.Portabilidade;
import com.sif.repository.PortabilidadeRepository;
import com.sif.repository.specification.PortabilidadeSpecification;

@Service
public class PortabilidadeService {

	@Autowired
	PortabilidadeRepository portabilidadeRepository;
	
	public ResponseEntity<List<Portabilidade>> getAll() {
		return Optional
				.ofNullable(portabilidadeRepository.findAll(new PortabilidadeSpecification().findByStatus()))
				.map(portabilidade -> ResponseEntity.ok().body(portabilidade))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Portabilidade> findById(Long id) {
		return Optional
				.ofNullable(portabilidadeRepository.findById(id).get())
				.map(portabilidade -> ResponseEntity.ok().body(portabilidade))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Portabilidade> create(Portabilidade portabilidade) {
		
		validarPortabilidade(portabilidade);
		
		return Optional
				.ofNullable(portabilidadeRepository.save(portabilidade))
				.map(portabilidadeAux -> ResponseEntity.ok().body(portabilidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Portabilidade> update(Portabilidade portabilidade,Long id) {
		
		validarPortabilidade(portabilidade);
		
		Portabilidade portabilidadeSave = portabilidadeRepository.findById(id).get();
		if(portabilidadeSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long portabilidadeId = portabilidadeSave.getId();
		
		BeanUtils.copyProperties(portabilidade, portabilidadeSave);
		portabilidadeSave.setId(portabilidadeId);
		
		return Optional
				.ofNullable(portabilidadeRepository.save(portabilidadeSave))
				.map(portabilidadeAux -> ResponseEntity.ok().body(portabilidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Portabilidade> delete(Long id) {
		
		Portabilidade portabilidadeSave = portabilidadeRepository.findById(id).get();
		if(portabilidadeSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		portabilidadeRepository.delete(portabilidadeSave);
		
		return ResponseEntity.ok().build();
	}
	
	private void validarPortabilidade(Portabilidade portabilidade) throws GenericException {
		
		if(portabilidade.getAverbadora() == null) {
			throw new GenericException("Erro","Averbadora não pode ser nulo");
		}
		if(portabilidade.getConsignataria() == null) {
			throw new GenericException("Erro","Consignataria não pode ser nulo");
		}
		if(portabilidade.getContrato() == null) {
			throw new GenericException("Erro","Contrato não pode ser nulo");
		}
	}	
	
}

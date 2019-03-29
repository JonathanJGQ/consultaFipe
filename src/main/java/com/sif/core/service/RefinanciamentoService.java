package com.sif.core.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.model.Refinanciamento;
import com.sif.repository.RefinanciamentoRepository;
import com.sif.repository.specification.RefinanciamentoSpecification;

@Service
public class RefinanciamentoService {

	@Autowired
	RefinanciamentoRepository refinanciamentoRepository;
	
	public ResponseEntity<List<Refinanciamento>> getAll() {
		return Optional
				.ofNullable(refinanciamentoRepository.findAll(new RefinanciamentoSpecification().findByStatus()))
				.map(refinanciamento -> ResponseEntity.ok().body(refinanciamento))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Refinanciamento> findById(Long id) {
		return Optional
				.ofNullable(refinanciamentoRepository.findById(id).get())
				.map(refinanciamento -> ResponseEntity.ok().body(refinanciamento))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Refinanciamento> create(Refinanciamento refinanciamento) {
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		
		refinanciamento.setDataCadastro(today);
		
		validarRefinanciamento(refinanciamento);
		
		return Optional
				.ofNullable(refinanciamentoRepository.save(refinanciamento))
				.map(refinanciamentoAux -> ResponseEntity.ok().body(refinanciamentoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Refinanciamento> update(Refinanciamento refinanciamento,Long id) {
		
		validarRefinanciamento(refinanciamento);
		
		Refinanciamento refinanciamentoSave = refinanciamentoRepository.findById(id).get();
		if(refinanciamentoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long refinanciamentoId = refinanciamentoSave.getId();
		
		BeanUtils.copyProperties(refinanciamento, refinanciamentoSave);
		refinanciamentoSave.setId(refinanciamentoId);
		
		return Optional
				.ofNullable(refinanciamentoRepository.save(refinanciamentoSave))
				.map(refinanciamentoAux -> ResponseEntity.ok().body(refinanciamentoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<Refinanciamento> delete(Long id) {
		
		Refinanciamento refinanciamentoSave = refinanciamentoRepository.findById(id).get();
		if(refinanciamentoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		refinanciamentoRepository.delete(refinanciamentoSave);
		
		return ResponseEntity.ok().build();
	}
	
	private void validarRefinanciamento(Refinanciamento refinanciamento) throws GenericException {
		
		if(refinanciamento.getContrato() == null) {
			throw new GenericException("Erro","Contrato não pode ser nulo");
		}
		if(refinanciamento.getIdNovo() == null) {
			throw new GenericException("Erro","Id Novo não pode ser nulo");
		}
	}	
	
}

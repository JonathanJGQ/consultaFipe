package com.sif.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sif.core.exception.GenericException;
import com.sif.model.FuncionarioMargem;
import com.sif.repository.FuncionarioMargemRepository;
import com.sif.repository.specification.FuncionarioMargemSpecification;

@Service
public class FuncionarioMargemService {

	@Autowired
	FuncionarioMargemRepository clienteMargemRepository;
	
	public ResponseEntity<List<FuncionarioMargem>> getAll() {
		return Optional
				.ofNullable(clienteMargemRepository.findAll(new FuncionarioMargemSpecification().findByStatus()))
				.map(clienteMargem -> ResponseEntity.ok().body(clienteMargem))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<FuncionarioMargem> findById(Long id) {
		return Optional
				.ofNullable(clienteMargemRepository.findById(id).get())
				.map(endereco -> ResponseEntity.ok().body(endereco))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public FuncionarioMargem findByMargemFuncionario(Long idMargem, Long idFuncionario) {
		FuncionarioMargemSpecification funcMargemSpec = new FuncionarioMargemSpecification();
		
		List<FuncionarioMargem> funcionariosMargens = clienteMargemRepository.findAll(
			Specification.where(funcMargemSpec.findByStatus())
							.and(funcMargemSpec.idMargemEq(idMargem))
							.and(funcMargemSpec.idFuncionarioEq(idFuncionario))
		);
		
		if(funcionariosMargens != null && !funcionariosMargens.isEmpty()) {
			return funcionariosMargens.get(0);
		}
		
		return null;
	}
	
	public ResponseEntity<FuncionarioMargem> create(FuncionarioMargem clienteMargem) {
		
		validarClienteMargem(clienteMargem);
		
		return Optional
				.ofNullable(clienteMargemRepository.save(clienteMargem))
				.map(enderecoAux -> ResponseEntity.ok().body(enderecoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<FuncionarioMargem> update(FuncionarioMargem clienteMargem,Long id) {
		
		validarClienteMargem(clienteMargem);
		
		FuncionarioMargem clienteMargemSave = clienteMargemRepository.findById(id).get();
		if(clienteMargemSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long clienteMargemId = clienteMargemSave.getId();
		
		BeanUtils.copyProperties(clienteMargem, clienteMargemSave);
		clienteMargemSave.setId(clienteMargemId);
		
		return Optional
				.ofNullable(clienteMargemRepository.save(clienteMargemSave))
				.map(enderecoAux -> ResponseEntity.ok().body(enderecoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<FuncionarioMargem> delete(Long id) {
		
		FuncionarioMargem clienteMargemSave = clienteMargemRepository.findById(id).get();
		if(clienteMargemSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		clienteMargemSave.setStatus(false);
		
		return Optional
				.ofNullable(clienteMargemRepository.save(clienteMargemSave))
				.map(enderecoAux -> ResponseEntity.ok().body(enderecoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public FuncionarioMargem save(FuncionarioMargem funcionarioMargem) {
		return clienteMargemRepository.save(funcionarioMargem);
	}
	
	private void validarClienteMargem(FuncionarioMargem clienteMargem) throws GenericException {
		
		if(clienteMargem.getFuncionario() == null) {
			throw new GenericException("Erro","Cliente não pode ser nulo");
		}
		if(clienteMargem.getMargem() == null) {
			throw new GenericException("Erro","Margem não pode ser nulo");
		}
		if(clienteMargem.getPercentualClienteMargem() == null) {
			throw new GenericException("Erro","Percentual não pode ser nulo");
		}
		if(clienteMargem.getLimite() == null) {
			throw new GenericException("Erro","Limite não pode ser nulo");
		}
		if(clienteMargem.getMaximaParcelaCliente() == null) {
			throw new GenericException("Erro","Parcela Máxima não pode ser nulo");
		}
	}
	
	@Transactional
	public void saveList(List<FuncionarioMargem> funcionariosMargens) {
		
		for(FuncionarioMargem funcionarioMargem : funcionariosMargens) {
			
			clienteMargemRepository.save(funcionarioMargem);
			
		}
		
	}
	
}

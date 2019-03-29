package com.sif.core.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.model.Funcionario;
import com.sif.model.FuncionarioSecretaria;
import com.sif.repository.FuncionarioSecretariaRepository;
import com.sif.repository.specification.FuncionarioSecretariaSpecification;

@Service
public class FuncionarioSecretariaService {

	@Autowired
	FuncionarioSecretariaRepository clienteSecretariaRepository;
	
	public ResponseEntity<List<FuncionarioSecretaria>> getAll() {
		return Optional
				.ofNullable(clienteSecretariaRepository.findAll(new FuncionarioSecretariaSpecification().findByStatus()))
				.map(clienteSecretaria -> ResponseEntity.ok().body(clienteSecretaria))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<FuncionarioSecretaria> findById(Long id) {
		return Optional
				.ofNullable(clienteSecretariaRepository.findById(id).get())
				.map(clienteSecretaria -> ResponseEntity.ok().body(clienteSecretaria))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public List<FuncionarioSecretaria> findByFuncionario(Funcionario funcionario) {
		
		FuncionarioSecretariaSpecification funcSecSpec = new FuncionarioSecretariaSpecification();
		
		List<FuncionarioSecretaria> funcSecretarias = clienteSecretariaRepository.findAll(
			Specification.where(funcSecSpec.findByFuncionario(funcionario.getId()))
		);
		
		return funcSecretarias;
		
	}
	
	public ResponseEntity<FuncionarioSecretaria> create(FuncionarioSecretaria clienteSecretaria) {
		
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		clienteSecretaria.setDataCadastro(today);
		
		validarClienteSecretaria(clienteSecretaria);
		
		return Optional
				.ofNullable(clienteSecretariaRepository.save(clienteSecretaria))
				.map(enderecoAux -> ResponseEntity.ok().body(enderecoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<FuncionarioSecretaria> update(FuncionarioSecretaria clienteSecretaria,Long id) {
		
		validarClienteSecretaria(clienteSecretaria);
		
		FuncionarioSecretaria clienteSecretariaSave = clienteSecretariaRepository.findById(id).get();
		if(clienteSecretariaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long clienteSecretariaId = clienteSecretariaSave.getId();
		
		BeanUtils.copyProperties(clienteSecretaria, clienteSecretariaSave);
		clienteSecretariaSave.setId(clienteSecretariaId);
		
		return Optional
				.ofNullable(clienteSecretariaRepository.save(clienteSecretariaSave))
				.map(enderecoAux -> ResponseEntity.ok().body(enderecoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<FuncionarioSecretaria> delete(Long id) {
		
		FuncionarioSecretaria clienteSecretariaSave = clienteSecretariaRepository.findById(id).get();
		if(clienteSecretariaSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		clienteSecretariaSave.setStatus(false);
		
		return Optional
				.ofNullable(clienteSecretariaRepository.save(clienteSecretariaSave))
				.map(enderecoAux -> ResponseEntity.ok().body(enderecoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	private void validarClienteSecretaria(FuncionarioSecretaria clienteSecretaria) throws GenericException {
		
		if(clienteSecretaria.getFuncionario() == null) {
			throw new GenericException("Erro","Funcionario não pode ser nulo");
		}
		if(clienteSecretaria.getSecretaria() == null) {
			throw new GenericException("Erro","Secretaria não pode ser nulo");
		}
	}	
	
}

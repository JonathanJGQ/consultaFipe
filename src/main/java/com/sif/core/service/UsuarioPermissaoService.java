package com.sif.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.core.exception.GenericException;
import com.sif.model.UsuarioPermissao;
import com.sif.repository.UsuarioPermissaoRepository;
import com.sif.repository.specification.UsuarioPermissaoSpecification;

@Service
public class UsuarioPermissaoService {

	@Autowired
	UsuarioPermissaoRepository usuarioPermissaoRepository;
	
	public ResponseEntity<List<UsuarioPermissao>> getAll() {
		return Optional
				.ofNullable(usuarioPermissaoRepository.findAll(new UsuarioPermissaoSpecification().findByStatus()))
				.map(portabilidade -> ResponseEntity.ok().body(portabilidade))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<UsuarioPermissao> findById(Long id) {
		return Optional
				.ofNullable(usuarioPermissaoRepository.findById(id).get())
				.map(portabilidade -> ResponseEntity.ok().body(portabilidade))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<UsuarioPermissao> create(UsuarioPermissao usuarioPermissao) {
		
		validarUsuarioPermissao(usuarioPermissao);
		
		return Optional
				.ofNullable(usuarioPermissaoRepository.save(usuarioPermissao))
				.map(usuarioPermissaoAux -> ResponseEntity.ok().body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<UsuarioPermissao> update(UsuarioPermissao usuarioPermissao,Long id) {
		
		validarUsuarioPermissao(usuarioPermissao);
		
		UsuarioPermissao usuarioPermissaoSave = usuarioPermissaoRepository.findById(id).get();
		if(usuarioPermissaoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long usuarioPermissaoId = usuarioPermissaoSave.getId();
		
		BeanUtils.copyProperties(usuarioPermissao, usuarioPermissaoSave);
		usuarioPermissaoSave.setId(usuarioPermissaoId);
		
		return Optional
				.ofNullable(usuarioPermissaoRepository.save(usuarioPermissaoSave))
				.map(usuarioPermissaoAux -> ResponseEntity.ok().body(usuarioPermissaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<UsuarioPermissao> delete(Long id) {
		
		UsuarioPermissao usuarioPermissaoSave = usuarioPermissaoRepository.findById(id).get();
		if(usuarioPermissaoSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		usuarioPermissaoRepository.delete(usuarioPermissaoSave);
		
		return ResponseEntity.ok().build();
	}
	
	private void validarUsuarioPermissao(UsuarioPermissao usuarioPermissao) throws GenericException {
		
		if(usuarioPermissao.getFuncionalidade() == null) {
			throw new GenericException("Erro","Funcionalidade não pode ser nulo");
		}
		if(usuarioPermissao.getUsuario() == null) {
			throw new GenericException("Erro","Usuário não pode ser nulo");
		}
		if(usuarioPermissao.getData() == null) {
			throw new GenericException("Erro","Data não pode ser nulo");
		}
	}	
	
}

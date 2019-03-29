package com.sif.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sif.model.PerfilFuncionalidade;
import com.sif.repository.PerfilFuncionalidadeRepository;
import com.sif.repository.specification.PerfilFuncionalidadeSpecification;

@Service
public class PerfilFuncionalidadeService {
	
	@Autowired
	PerfilFuncionalidadeRepository perfilFuncionalidadeRepository;
	
	public List<PerfilFuncionalidade> getAll() {
		return perfilFuncionalidadeRepository.findAll();
	}
	
	public ResponseEntity<PerfilFuncionalidade> findById(Long id) {
		
		return Optional
				.ofNullable(perfilFuncionalidadeRepository.findById(id).get())
				.map(perfilFuncionalidadeAux -> ResponseEntity.ok().body(perfilFuncionalidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<PerfilFuncionalidade> create(PerfilFuncionalidade perfilFuncionalidade) {
		return Optional
				.ofNullable(perfilFuncionalidadeRepository.save(perfilFuncionalidade))
				.map(perfilFuncionalidadeAux -> ResponseEntity.ok().body(perfilFuncionalidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<PerfilFuncionalidade> update(PerfilFuncionalidade perfilFuncionalidade,Long id) {
		
		PerfilFuncionalidade perfilFuncionalidadeSave = perfilFuncionalidadeRepository.findById(id).get();
		if(perfilFuncionalidadeSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		Long idOrgao = perfilFuncionalidadeSave.getId();
		
		BeanUtils.copyProperties(perfilFuncionalidade, perfilFuncionalidadeSave);
		perfilFuncionalidadeSave.setId(idOrgao);
		
		return Optional
				.ofNullable(perfilFuncionalidadeRepository.save(perfilFuncionalidadeSave))
				.map(perfilFuncionalidadeAux -> ResponseEntity.ok().body(perfilFuncionalidadeAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public ResponseEntity<PerfilFuncionalidade> delete(Long id) {
		
		PerfilFuncionalidade perfilFuncionalidadeSave = perfilFuncionalidadeRepository.findById(id).get();
		if(perfilFuncionalidadeSave == null) {
			return ResponseEntity.notFound().build();
		}
		
		perfilFuncionalidadeSave.setStatus(false);
		
		return Optional
				.ofNullable(perfilFuncionalidadeRepository.save(perfilFuncionalidadeSave))
				.map(orgaoAux -> ResponseEntity.ok().body(orgaoAux))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	public void forceDelete(Long id) {
		
		PerfilFuncionalidade perfilFuncionalidadeSave = perfilFuncionalidadeRepository.findById(id).get();
		if(perfilFuncionalidadeSave == null) {
			return;
		}
		
		perfilFuncionalidadeRepository.delete(perfilFuncionalidadeSave);
	}
	
	public List<PerfilFuncionalidade> findByFuncionalidadeEPerfil(Long idFuncionalidade, Long idPerfil) {
		
		PerfilFuncionalidadeSpecification perfilFuncionalidadeSpecification = new PerfilFuncionalidadeSpecification();
		
		return perfilFuncionalidadeRepository.findAll(Specification.where(
			perfilFuncionalidadeSpecification.idFuncionalidadeEPefilEq(idFuncionalidade, idPerfil)
		));
	}
	
	public List<PerfilFuncionalidade> findByFuncionalidade(Long idFuncionalidade) {
		
		PerfilFuncionalidadeSpecification perfilFuncionalidadeSpecification = new PerfilFuncionalidadeSpecification();
		
		return perfilFuncionalidadeRepository.findAll(Specification.where(
			perfilFuncionalidadeSpecification.idFuncionalidadeEq(idFuncionalidade)
		));
	}
}

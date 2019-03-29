package com.sif.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.service.AdministracaoService;
import com.sif.model.Administracao;
import com.sif.model.custom.AdministracaoCustomDTO;


@RestController
@RequestMapping("/administracao")
public class AdministracaoController{
	
	@Autowired
	AdministracaoService administracaoService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/administracao/list')")
	public Page<Administracao> getAll(Pageable pageable, Administracao administracao) {
		return administracaoService.getAll(pageable, administracao);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/administracao/list')")
	public ResponseEntity<AdministracaoCustomDTO> findById(@PathVariable() Long id) {
		return administracaoService.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/administracao/new')")
	public ResponseEntity<Administracao> create(@RequestBody AdministracaoCustomDTO administracao) {
		return administracaoService.create(administracao);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/administracao/edit')")
	public ResponseEntity<Administracao> update(@RequestBody AdministracaoCustomDTO administracaoDto, @PathVariable() Long id) {
		return administracaoService.update(administracaoDto, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/administracao/delete')")
	public ResponseEntity<Administracao> delete(@PathVariable() Long id) {
		return administracaoService.delete(id);
	}
}


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

import com.sif.core.service.FuncionalidadeService;
import com.sif.model.Funcionalidade;
import com.sif.model.custom.FuncionalidadeCustomDTO;

@RestController
@RequestMapping("/funcionalidade")
public class FuncionalidadeController{
	
	@Autowired
	FuncionalidadeService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/funcionalidade/list')")
	public Page<Funcionalidade> getAll(Pageable pageable, Funcionalidade funcionalidade) {
		return service.getAll(pageable, funcionalidade);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/funcionalidade/list')")
	public ResponseEntity<FuncionalidadeCustomDTO> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/funcionalidade/new')")
	public ResponseEntity<Funcionalidade> create(@RequestBody FuncionalidadeCustomDTO funcionalidade) {
		return service.create(funcionalidade);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/funcionalidade/edit')")
	public ResponseEntity<Funcionalidade> update(@RequestBody FuncionalidadeCustomDTO funcionalidade, @PathVariable() Long id) {
		return service.update(funcionalidade, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/funcionalidade/delete')")
	public ResponseEntity<Funcionalidade> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}


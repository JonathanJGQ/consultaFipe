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

import com.sif.core.service.PerfilService;
import com.sif.model.Perfil;

@RestController
@RequestMapping("/perfil")
public class PerfilController{
	
	@Autowired
	PerfilService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/perfil/list')")
	public Page<Perfil> getAll(Pageable pageable, Perfil perfil) {
		return service.getAll(pageable, perfil);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/perfil/list')")
	public ResponseEntity<Perfil> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/perfil/new')")
	public ResponseEntity<Perfil> create(@RequestBody Perfil perfil) {
		return service.create(perfil);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/perfil/edit')")
	public ResponseEntity<Perfil> update(@RequestBody Perfil perfil, @PathVariable() Long id) {
		return service.update(perfil, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/perfil/delete')")
	public ResponseEntity<Perfil> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}


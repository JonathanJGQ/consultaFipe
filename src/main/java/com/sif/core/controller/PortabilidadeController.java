package com.sif.core.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.sif.core.service.PortabilidadeService;
import com.sif.model.Portabilidade;

@RestController
@RequestMapping("/portabilidade")
public class PortabilidadeController{
	
	@Autowired
	PortabilidadeService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/portabilidade/list')")
	public ResponseEntity<List<Portabilidade>> getAll() {
		return service.getAll();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/portabilidade/list')")
	public ResponseEntity<Portabilidade> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/portabilidade/new')")
	public ResponseEntity<Portabilidade> create(@RequestBody Portabilidade portabilidade) {
		return service.create(portabilidade);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/portabilidade/edit')")
	public ResponseEntity<Portabilidade> update(@RequestBody Portabilidade portabilidade, @PathVariable() Long id) {
		return service.update(portabilidade, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/portabilidade/delete')")
	public ResponseEntity<Portabilidade> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}


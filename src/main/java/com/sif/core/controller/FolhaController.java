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

import com.sif.core.service.FolhaService;
import com.sif.model.Folha;

@RestController
@RequestMapping("/folha")
public class FolhaController{
	
	@Autowired
	FolhaService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/folha/list')")
	public ResponseEntity<List<Folha>> getAll() {
		return service.getAll();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/folha/list')")
	public ResponseEntity<Folha> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/folha/new')")
	public ResponseEntity<Folha> create(@RequestBody Folha folha) {
		return service.create(folha);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/folha/edit')")
	public ResponseEntity<Folha> update(@RequestBody Folha folha, @PathVariable() Long id) {
		return service.update(folha, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/folha/delete')")
	public ResponseEntity<Folha> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}


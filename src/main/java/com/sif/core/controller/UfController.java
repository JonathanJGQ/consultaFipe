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

import com.sif.core.service.UfService;
import com.sif.model.Uf;

@RestController
@RequestMapping("/uf")
public class UfController{
	
	@Autowired
	UfService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/uf/list')")
	public Page<Uf> getAll(Pageable pageable, Uf uf) {
		return service.getAll(pageable, uf);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/uf/list')")
	public ResponseEntity<Uf> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/uf/new')")
	public ResponseEntity<Uf> create(@RequestBody Uf uf) {
		return service.create(uf);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/uf/edit')")
	public ResponseEntity<Uf> update(@RequestBody Uf uf, @PathVariable() Long id) {
		return service.update(uf, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/uf/delete')")
	public ResponseEntity<Uf> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}


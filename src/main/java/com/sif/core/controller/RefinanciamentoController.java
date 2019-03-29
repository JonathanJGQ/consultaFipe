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

import com.sif.core.service.RefinanciamentoService;
import com.sif.model.Refinanciamento;

@RestController
@RequestMapping("/refinanciamento")
public class RefinanciamentoController{
	
	@Autowired
	RefinanciamentoService service;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/refinanciamento/list')")
	public ResponseEntity<List<Refinanciamento>> getAll() {
		return service.getAll();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/refinanciamento/list')")
	public ResponseEntity<Refinanciamento> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/refinanciamento/new')")
	public ResponseEntity<Refinanciamento> create(@RequestBody Refinanciamento refinanciamento) {
		return service.create(refinanciamento);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/refinanciamento/edit')")
	public ResponseEntity<Refinanciamento> update(@RequestBody Refinanciamento refinanciamento, @PathVariable() Long id) {
		return service.update(refinanciamento, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/refinanciamento/delete')")
	public ResponseEntity<Refinanciamento> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}


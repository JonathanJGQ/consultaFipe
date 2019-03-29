package com.sif.core.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sif.core.service.FuncionarioMargemService;
import com.sif.model.FuncionarioMargem;

@RestController
@RequestMapping("/funcionariomargem")
public class FuncionarioMargemController{
	
	@Autowired
	FuncionarioMargemService service;
	
	@GetMapping
	public ResponseEntity<List<FuncionarioMargem>> getAll() {
		return service.getAll();
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<FuncionarioMargem> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	public ResponseEntity<FuncionarioMargem> create(@RequestBody FuncionarioMargem clienteMargem) {
		return service.create(clienteMargem);
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<FuncionarioMargem> update(@RequestBody FuncionarioMargem clienteMargem, @PathVariable() Long id) {
		return service.update(clienteMargem, id);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<FuncionarioMargem> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}


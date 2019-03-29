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

import com.sif.core.service.FuncionarioSecretariaService;
import com.sif.model.FuncionarioSecretaria;

@RestController
@RequestMapping("/clientesecretaria")
public class FuncionarioSecretariaController{
	
	@Autowired
	FuncionarioSecretariaService service;
	
	@GetMapping
	public ResponseEntity<List<FuncionarioSecretaria>> getAll() {
		return service.getAll();
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<FuncionarioSecretaria> findById(@PathVariable() Long id) {
		return service.findById(id);
	}
	
	@PostMapping
	public ResponseEntity<FuncionarioSecretaria> create(@RequestBody FuncionarioSecretaria clienteSecretaria) {
		return service.create(clienteSecretaria);
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<FuncionarioSecretaria> update(@RequestBody FuncionarioSecretaria clienteSecretaria, @PathVariable() Long id) {
		return service.update(clienteSecretaria, id);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<FuncionarioSecretaria> delete(@PathVariable() Long id) {
		return service.delete(id);
	}
}


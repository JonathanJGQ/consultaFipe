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

import com.sif.core.service.EnderecoService;
import com.sif.model.Endereco;

@RestController
@RequestMapping("/endereco")
public class EnderecoController{
	
	@Autowired
	EnderecoService enderecoService;
	
	@GetMapping
	@PreAuthorize("hasAuthority('/endereco/list')")
	public ResponseEntity<List<Endereco>> getAll() {
		return enderecoService.getAll();
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('/endereco/list')")
	public ResponseEntity<Endereco> findById(@PathVariable() Long id) {
		return enderecoService.findById(id);
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('/endereco/new')")
	public ResponseEntity<Endereco> create(@RequestBody Endereco endereco) {
		return enderecoService.create(endereco);
	}
	
	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('/endereco/edit')")
	public ResponseEntity<Endereco> update(@RequestBody Endereco endereco, @PathVariable() Long id) {
		return enderecoService.update(endereco, id);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('/endereco/delete')")
	public ResponseEntity<Endereco> delete(@PathVariable() Long id) {
		return enderecoService.delete(id);
	}
}

